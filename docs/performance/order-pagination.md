# Référence de performance SQL — pagination des commandes (avant optimisation)

## Objectif et périmètre

Le point d’entrée HTTP étudié est `GET /user/order/page`. Le contrôleur appelle `OrderServiceImpl.pageQuery4User` : PageHelper exécute une requête COUNT et la requête paginée, puis le service charge les détails une commande à la fois avec `OrderDetailMapper.getByOrderId`. Le parcours comprend donc une requête COUNT, une requête de commandes et une requête de détails par commande retournée. La recherche administrative présente aussi une lecture par commande ; cette mesure porte sur le parcours utilisateur, actif pour tous les états.

Le test utilise les mappers MyBatis et la configuration PageHelper existants du projet. Il mesure leur parcours SQL réel sans modifier la logique métier. Le temps de génération des données et celui des commandes `EXPLAIN` sont exclus.

## Données et conditions de requête

- Base dédiée : `sky_take_out_perf_test`. Le script de préparation refuse tout autre nom de base et le test JDBC vérifie l’URL avant de se connecter.
- Données : 10 000 commandes, toutes associées à l’utilisateur `9001`, et 30 000 détails, soit trois détails par commande.
- Valeurs textuelles : `Test User`, `Performance Test Address` et `Test Dish-1` à `Test Dish-3`. Les formats des numéros, les montants, les états, les quantités et leur distribution restent identiques aux règles de génération précédentes.
- Filtre : utilisateur `9001`, sans filtre d’état, de paiement, de date, de numéro de commande ou de téléphone ; tri `order_time DESC`, première page.
- Tailles de page : 10, 20 et 50 commandes.
- Index : clé primaire et contrainte unique sur `orders.number`, index `orders.idx_orders_user_time (user_id, order_time)` et index `order_detail.idx_order_detail_order_id (order_id)`.

## Environnement et méthode retenus

- Mesure finale : 10 octobre 2026 à 10:00 UTC
- MySQL Server 9.2.0, service Homebrew local, `127.0.0.1:3306`
- JDK 17.0.18 ; macOS sur Apple Silicon (`aarch64`) ; 8 processeurs disponibles pour la JVM
- JDBC : `jdbc:mysql://127.0.0.1:3306/sky_take_out_perf_test`, fuseau horaire `Asia/Shanghai`
- MyBatis 2.2.0 et PageHelper 1.3.0 ; application web Spring non démarrée
- Préparation : 30 exécutions de préchauffage par taille de page
- Mesure : 100 mesures par taille de page, en alternant les ordres `10, 20, 50`, `20, 50, 10` et `50, 10, 20`
- SQL : le test compte les instructions réellement préparées via `StatementHandler.prepare` et distingue la requête COUNT, la page de commandes et les détails
- Statistiques : médiane des deux valeurs centrales pour 100 échantillons et P95 nearest-rank ; les 100 latences restent dans leur ordre d’observation, sans exclusion des valeurs extrêmes

Les latences et les plans `EXPLAIN` complets sont disponibles dans le [rapport JSON de référence](order-pagination-baseline.json). Le JSON conserve aussi le premier relevé de 40 échantillons. Celui-ci présentait une dispersion importante ; les exécutions de préchauffage et le nombre de mesures ont été augmentés, et l’ordre d’exécution a été alterné pour établir la référence retenue.

## Reproduction

Les commandes ci-dessous utilisent uniquement `sky_take_out_perf_test`. La préparation efface et recrée les deux tables de cette base. Elle est nécessaire pour reconstruire le jeu de données depuis zéro ; pour rejouer uniquement la mesure, ne lancez pas le script. Le mode intercalé, utilisé par défaut, s’exécute seul : un rapport précédent est facultatif et, s’il existe au chemin de sortie, il est conservé dans le nouveau JSON.

```bash
export SKY_ORDER_PAGE_DB_HOST=127.0.0.1
export SKY_ORDER_PAGE_DB_PORT=3306
export SKY_ORDER_PAGE_DB_NAME=sky_take_out_perf_test
export SKY_ORDER_PAGE_DB_USER=root
export SKY_ORDER_PAGE_DB_PASSWORD='mot de passe local'

# À exécuter uniquement pour reconstruire les données du schéma dédié
./scripts/prepare-order-pagination-baseline.sh

export SKY_ORDER_PAGE_DB_URL='jdbc:mysql://127.0.0.1:3306/sky_take_out_perf_test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true'

# Exécutions de préchauffage : 30 par taille de page ; mesures par taille de page : 100
mvn -pl sky-server -am \
  -Dtest=OrderPaginationBaselineMySqlTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dorder.pagination.baseline.output=../target/order-pagination-local.json \
  test
```

Le test accepte uniquement une URL JDBC dont le schéma est exactement `sky_take_out_perf_test`. La préparation est séparée des mesures. Pour comparer ultérieurement la version optimisée, conservez les mêmes données, filtres, tailles de page, exécutions de préchauffage, alternance et nombre de mesures.

## Référence avant optimisation

Le relevé ci-dessous est la mesure finale retenue : 30 exécutions de préchauffage et 100 mesures par taille de page. Chaque réponse contient autant de commandes que demandé.

| Commandes par page | Requête COUNT | Requête de page | Requêtes de détails | Total SQL | Médiane | P95 |
|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1 | 1 | 10 | 12 | 10,398 ms | 11,138 ms |
| 20 | 1 | 1 | 20 | 22 | 12,666 ms | 13,713 ms |
| 50 | 1 | 1 | 50 | 52 | 18,663 ms | 20,251 ms |

Le total SQL inclut bien la requête COUNT de PageHelper : une requête COUNT, une requête paginée et une requête de détails par commande retournée.

## Plans d’exécution

Les plans ci-dessous sont ceux de la mesure finale ; leurs lignes complètes figurent dans le JSON.

- La requête COUNT et la requête de page utilisent `orders.idx_orders_user_time`.
- La page utilise un parcours d’index descendant (« backward index scan »), sans tri `filesort`.
- MySQL transforme la condition `EXISTS` en semi-jointure `FirstMatch(orders)` et accède aux détails par `order_detail.idx_order_detail_order_id` ; l’estimation est de deux détails par commande.
- La requête de détails d’une commande utilise également `idx_order_detail_order_id`, avec trois lignes estimées.

## Limites

La latence mesure le parcours des requêtes Mapper, y compris l’acquisition de session MyBatis et les allers-retours JDBC locaux. **Elle ne mesure pas le temps de réponse HTTP complet** et exclut le contrôleur, l’authentification, la sérialisation JSON et le réseau client. Le test est exécuté localement sans charge concurrente ; ses résultats ne représentent pas une charge de production.
