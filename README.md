# Sky Take Out

## Configuration locale

1. Copiez `.env.example` vers `.env`.
2. Renseignez dans `.env` les paramètres locaux de la base de données, de JWT et d’OSS.
3. Dans VS Code, lancez la configuration de démarrage `SkyApplication`.

Le fichier `.env` contient des identifiants locaux et est volontairement exclu de Git. La configuration de démarrage VS Code `SkyApplication` utilise explicitement le profil `dev` et charge ce fichier.

### Clés JWT

L’application nécessite deux clés de signature HS256 indépendantes dans le fichier local `.env` : `JWT_ADMIN_SECRET_KEY` et `JWT_USER_SECRET_KEY`. Générez chaque valeur une seule fois sur macOS avec :

```sh
openssl rand -base64 32 | tr -d '\n'
```

Exécutez la commande séparément pour chaque variable, puis placez les deux résultats dans les entrées correspondantes de `.env`. La commande génère 32 octets aléatoires et les encode en Base64. Conservez `.env` en local, ne copiez aucune des deux valeurs dans des fichiers suivis par Git ou dans des journaux, et ne régénérez pas les clés à chaque démarrage de l’application : tout changement invalide les jetons signés avec la valeur précédente. Le démarrage échoue avec une erreur de configuration si une clé est absente, mal formée, trop courte ou identique à l’autre.

### Paiement simulé

Le point d’accès de paiement simulé est activé uniquement par le profil local `dev`. La configuration de base le désactive, tout comme le profil `prod`, même si `dev` est activé accidentellement en même temps. Utilisez la configuration de démarrage VS Code fournie pour tester localement le parcours de commande. Les déploiements en production doivent activer explicitement `prod` ; l’absence de configuration réelle pour le paiement WeChat n’active pas le paiement simulé et n’entraîne aucun repli vers celui-ci.

## Vérification de la concurrence des changements de statut des commandes

Les tests Mockito vérifient les décisions du service et les arguments des mises à jour conditionnelles sans se connecter à une base de données. Le test d’intégration MySQL distinct charge le véritable `OrderMapper.xml` du projet et met en concurrence la confirmation du paiement et l’annulation pour expiration, dans des transactions et connexions indépendantes. Il vérifie qu’une mise à jour l’emporte, que l’autre ne modifie aucune ligne, que les états finaux de la commande et du paiement concordent, qu’une commande dont l’horodatage correspond exactement à la limite d’expiration n’est pas annulée et que des transitions répétées ne remplacent pas un état terminal. Il s’agit d’un test de correction, pas d’un test de charge ou de performance.

Le test a été vérifié avec MySQL 9.2.0 en local. Chaque tour de concurrence simultanée insère une ligne au nom unique dans `sky_take_out_test.orders`, puis la supprime ; la base de données de l’application `sky_take_out` n’est pas utilisée. Le test exécute également deux vérifications avec un ordre coordonné afin de couvrir de manière déterministe les deux scénarios gagnants, sans que les 100 assertions de concurrence simultanée dépendent de l’équité de l’ordonnanceur.

### Résultats des tests

| Vérification | Résultat |
| --- | ---: |
| Version de MySQL | 9.2.0 |
| Concurrences simultanées formelles | 100/100 réussies |
| Victoires du paiement / de l’annulation pour expiration | 72 / 28 |
| États finaux commande/paiement incohérents | 0 |
| Scénarios déterministes paiement d’abord et expiration d’abord | Les deux réussis |

Pour effectuer la vérification préalable en 10 tours, utilisez la même commande ci-dessous en définissant `SKY_ORDER_RACE_ROUNDS=10`. Le nombre de victoires varie selon l’ordonnancement des threads ; chaque tour doit néanmoins comporter exactement une mise à jour conditionnelle réussie. Les deux vérifications avec un ordre coordonné garantissent les deux états finaux possibles sans faire dépendre la correction de l’équité de l’ordonnanceur. Il s’agit de vérifications de correction, pas de tests de charge ou de performance.

Créez une seule fois la base et la table de test InnoDB isolées à l’aide du script fourni :

```sh
mysql --protocol=TCP -h localhost -P 3306 -u root -p < sky-server/src/test/resources/mysql/order-status-race-schema.sql
```

Chargez les valeurs locales existantes de `.env` dans le shell courant, puis définissez les variables de connexion au test. Conservez le mot de passe dans une variable d’environnement locale et ne l’inscrivez pas dans un fichier suivi par Git :

```sh
set -a
source ./.env
set +a
export SKY_ORDER_RACE_DB_URL='jdbc:mysql://localhost:3306/sky_take_out_test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true'
export SKY_ORDER_RACE_DB_USER='root'
export SKY_ORDER_RACE_DB_PASSWORD="$DB_PASSWORD"
```

Exécutez séparément la vérification préalable et le test formel :

```sh
SKY_ORDER_RACE_ROUNDS=10 mvn -pl sky-server -am -Dtest=OrderStatusRaceMySqlIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
SKY_ORDER_RACE_ROUNDS=100 mvn -pl sky-server -am -Dtest=OrderStatusRaceMySqlIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Le test d’intégration est ignoré si ses variables de connexion dédiées sont absentes et rejette toute URL JDBC qui ne cible pas `sky_take_out_test`. Il nécessite une table InnoDB `orders` créée par le script de schéma du test. L’exécution MySQL valide l’atomicité des mises à jour conditionnelles du mapper pour les scénarios de concurrence testés ; elle ne remplace pas des tests plus larges du service, du fournisseur de paiement ou de la charge en production.

## Optimisation N+1 de la pagination des commandes

La pagination utilisateur `GET /user/order/page` chargeait auparavant les détails avec une requête par commande. Le service conserve PageHelper pour le COUNT, les filtres et l’ordre de la page, puis charge en une seule requête les détails des ID de la page courante et les regroupe par commande.

Mesure locale sur MySQL 9.2.0, avec 10 000 commandes, 30 000 détails, 30 exécutions de préchauffage et 100 mesures intercalées par taille de page :

| Commandes par page | SQL avant → après | Médiane avant → après | P95 avant → après |
| ---: | ---: | ---: | ---: |
| 10 | 12 → 3 (−75,0 %) | 10,398 → 9,072 ms | 11,138 → 9,701 ms |
| 20 | 22 → 3 (−86,4 %) | 12,666 → 9,123 ms | 13,713 → 9,880 ms |
| 50 | 52 → 3 (−94,2 %) | 18,663 → 9,611 ms | 20,251 → 10,898 ms |

Les mesures avant et après optimisation ont été réalisées lors de sessions distinctes. Les différences de latence observées peuvent donc également être influencées par les variations de charge du système, de cache et d’environnement d’exécution. La réduction du nombre de requêtes SQL est vérifiée directement ; les gains de latence présentés correspondent aux résultats observés dans cet environnement de test local.

Ces latences mesurent le parcours Mapper local, pas le temps de réponse HTTP complet. Le protocole, les variations et les limites de la mesure figurent dans la [documentation de performance de la pagination](docs/performance/order-pagination.md), avec les échantillons et plans complets avant et après optimisation.
