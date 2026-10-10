# Performances SQL — pagination des commandes

## Objectif et périmètre

La mesure porte sur l’endpoint utilisateur `GET /user/order/page` et son service `OrderServiceImpl.pageQuery4User`. Avant optimisation, PageHelper exécutait une requête COUNT et la requête paginée, puis le service chargeait les détails une commande à la fois avec `OrderDetailMapper.getByOrderId`. Depuis Commit 2, le service récupère en une requête les détails des seules commandes de la page et les regroupe par commande. Le parcours administratif conserve son comportement propre et n’est pas mesuré ici.

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

Les commandes ci-dessous utilisent uniquement `sky_take_out_perf_test`. La préparation efface et recrée les deux tables de cette base. Elle n’est nécessaire que pour reconstruire le jeu de données depuis zéro ; ne la lancez pas pour reproduire la mesure enregistrée. Les exécutions de préchauffage et les mesures utilisent le mode intercalé avec 30 préchauffages et 100 mesures pour chacune des tailles 10, 20 et 50. La commande dédiée écrit le relevé optimisé dans un fichier distinct et ne lit ni ne remplace le JSON avant optimisation.

```bash
export SKY_ORDER_PAGE_DB_HOST=127.0.0.1
export SKY_ORDER_PAGE_DB_PORT=3306
export SKY_ORDER_PAGE_DB_NAME=sky_take_out_perf_test
export SKY_ORDER_PAGE_DB_USER=root
export SKY_ORDER_PAGE_DB_PASSWORD='mot de passe local'

# À exécuter uniquement pour reconstruire les données du schéma dédié ; ne pas lancer pour rejouer la mesure enregistrée
# ./scripts/prepare-order-pagination-baseline.sh

export SKY_ORDER_PAGE_DB_URL='jdbc:mysql://127.0.0.1:3306/sky_take_out_perf_test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true'

# Mesure optimisée : 30 exécutions de préchauffage et 100 mesures par taille de page
mvn -pl sky-server -am \
  -Dtest=OrderPaginationBaselineMySqlTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dorder.pagination.mode=interleaved \
  -Dorder.pagination.baseline.preservePrevious=false \
  -Dorder.pagination.baseline.output=../docs/performance/order-pagination-optimized.json \
  test
```

Le test accepte uniquement une URL JDBC dont le schéma est exactement `sky_take_out_perf_test`. Le chemin de sortie est résolu depuis le module `sky-server`. Le test crée son dossier parent si nécessaire. La préparation est séparée des mesures. Pour reproduire la comparaison, conservez les mêmes données, filtres, tailles de page, exécutions de préchauffage, alternance et nombre de mesures.

## Référence avant optimisation

Le relevé ci-dessous est la mesure finale retenue : 30 exécutions de préchauffage et 100 mesures par taille de page. Chaque réponse contient autant de commandes que demandé.

| Commandes par page | Requête COUNT | Requête de page | Requêtes de détails | Total SQL | Médiane | P95 |
|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1 | 1 | 10 | 12 | 10,398 ms | 11,138 ms |
| 20 | 1 | 1 | 20 | 22 | 12,666 ms | 13,713 ms |
| 50 | 1 | 1 | 50 | 52 | 18,663 ms | 20,251 ms |

Le total SQL inclut bien la requête COUNT de PageHelper : une requête COUNT, une requête paginée et une requête de détails par commande retournée.

## Comparaison avant/après

La mesure après optimisation a été réalisée le 10 octobre 2026 à 11:56 UTC, dans le même environnement, avec le même jeu de données, les mêmes filtres et le même parcours Mapper. Chaque série comprend 30 exécutions de préchauffage puis 100 mesures intercalées par taille de page. Le JSON optimisé conserve toutes les latences observées et les plans complets dans le [rapport JSON après optimisation](order-pagination-optimized.json).

| Commandes par page | SQL total avant → après | Réduction SQL | Détails avant → après | Médiane avant → après | Variation médiane | P95 avant → après | Variation P95 |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 12 → 3 | −75,0 % | 10 → 1 | 10,398 → 9,072 ms | −12,8 % | 11,138 → 9,701 ms | −12,9 % |
| 20 | 22 → 3 | −86,4 % | 20 → 1 | 12,666 → 9,123 ms | −28,0 % | 13,713 → 9,880 ms | −28,0 % |
| 50 | 52 → 3 | −94,2 % | 50 → 1 | 18,663 → 9,611 ms | −48,5 % | 20,251 → 10,898 ms | −46,2 % |

Pour chaque taille, le total après optimisation comprend une requête COUNT PageHelper, une requête de page et une requête groupée de détails. Les variations sont calculées par rapport à la valeur avant optimisation. Les échantillons extrêmes sont conservés ; les latences observées restent sensibles au bruit d’une machine locale et ne démontrent pas à elles seules un gain identique en production.

## Plans d’exécution avant optimisation

Les plans de référence complets figurent dans le [JSON avant optimisation](order-pagination-baseline.json).

- La requête COUNT et la requête de page utilisent `orders.idx_orders_user_time`.
- La page utilise un parcours d’index descendant (« backward index scan »), sans tri `filesort`.
- MySQL transforme la condition `EXISTS` en semi-jointure `FirstMatch(orders)` et accède aux détails par `order_detail.idx_order_detail_order_id` ; l’estimation est de deux détails par commande.
- La requête de détails d’une commande utilise également `idx_order_detail_order_id`, avec trois lignes estimées.

## Plans d’exécution après optimisation

Les plans après optimisation sont enregistrés dans le JSON dédié. Le COUNT et la page gardent les mêmes plans : `orders.idx_orders_user_time` pour les deux, avec un parcours descendant pour la page et la semi-jointure `FirstMatch(orders)` vers les détails. La requête groupée testée avec les 50 ID de la page utilise un accès `range` sur `order_detail.idx_order_detail_order_id`, avec 150 lignes estimées et `Using index condition` ; le plan ne signale pas de `filesort`.

Le nombre de requêtes de détails passe d’une requête par commande à une requête `IN (...)` sur les seuls ID de la page courante. Le test MySQL vérifie aussi que PageHelper a nettoyé son contexte après la requête de page et que le chargement des détails ne récupère pas de commandes d’une autre page.

## Limites

La latence mesure le parcours des requêtes Mapper, y compris l’acquisition de session MyBatis et les allers-retours JDBC locaux. **Elle ne mesure pas le temps de réponse HTTP complet** et exclut le contrôleur, l’authentification, la sérialisation JSON et le réseau client. Le test est exécuté localement sans charge concurrente ; ses résultats ne représentent pas une charge de production.
