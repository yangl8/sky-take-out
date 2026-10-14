# Installation locale

Ce guide décrit le démarrage local du back-office avec la base de démonstration fournie. Le dépôt ne fixe pas de version Java cible dans son POM ; le backend et les tests documentés ont été vérifiés avec JDK 17.0.18 et Maven 3.6.1. La compilation du back-office a été préparée avec Node.js 22.13.1 et npm 10.9.2. MySQL 8.0 ou ultérieur est requis par la collation `utf8mb4_0900_ai_ci` du schéma ; la base de mesure a été vérifiée sous MySQL 9.2. Redis doit être disponible localement. La compilation du mini-programme utilise HBuilderX, et non un script `build` npm présent dans ce dépôt.

## Périmètre du démarrage

Le back-office Vue et les fonctions de catalogue nécessitent le backend, MySQL, Redis et deux clés JWT locales. Les valeurs Cloudinary peuvent rester vides pour consulter les URL d’images déjà enregistrées ; elles sont nécessaires pour utiliser l’endpoint d’envoi d’image. Les parcours de connexion et de commande du mini-programme requièrent en plus un AppID/AppSecret WeChat valide et une clé AMap pour le contrôle de livraison.

## 1. Cloner le dépôt

```sh
git clone https://github.com/yangl8/sky-take-out.git
cd sky-take-out
```

Modules principaux : `sky-common`, `sky-pojo`, `sky-server`, `admin-web` et `mini-program`.

## 2. Créer une base de démonstration neuve

Les scripts `sql/schema.sql` et `sql/demo-data.sql` ciblent `sky_take_out_demo`. Le premier crée les 11 tables ; le second ajoute les données de démonstration. Les données comprennent 10 catégories, 24 plats, 13 associations de saveurs valides, 2 formules et 9 associations plat-formule.

Le schéma crée la base si elle n’existe pas, mais les instructions `CREATE TABLE` ne sont pas idempotentes. N’exécutez ces scripts que pour créer une base de démonstration neuve. Ne les relancez pas sur une base existante ou importante ; ils ne sont pas conçus pour supprimer, remplacer ou vider automatiquement des données.

Depuis la racine du dépôt, importez dans cet ordre :

```sh
mysql -h localhost -P 3306 -u root -p < sql/schema.sql
mysql -h localhost -P 3306 -u root -p < sql/demo-data.sql
```

Le mot de passe MySQL est demandé par le client interactif. Le compte de démonstration est réservé à un usage local.

## 3. Démarrer Redis et définir la configuration locale

Démarrez MySQL et Redis sur la même machine. La configuration `dev` attend MySQL à `localhost:3306`, Redis à `localhost:6379`, base Redis `0`, et l’utilisateur MySQL `root`. Les mots de passe sont lus dans les variables `DB_PASSWORD` et `REDIS_PASSWORD`.

Copiez le modèle puis éditez les valeurs localement :

```sh
cp .env.example .env
```

Les deux clés JWT doivent être des chaînes Base64 distinctes, chacune décodant en au moins 32 octets. Générez-les séparément :

```sh
openssl rand -base64 32 | tr -d '\n'
```

Inscrivez les résultats dans `JWT_ADMIN_SECRET_KEY` et `JWT_USER_SECRET_KEY` de `.env`. Ne réutilisez pas les mêmes clés et ne les ajoutez jamais aux fichiers suivis par Git.

Pour le back-office uniquement, les variables `WECHAT_APPID`, `WECHAT_SECRET`, `AMAP_KEY` et les identifiants Cloudinary peuvent rester vides dans `.env`. Les connexions WeChat, la validation de distance des commandes et l’envoi de nouveaux fichiers nécessitent les valeurs correspondantes.

Chargez `.env` dans le terminal qui lancera Spring Boot :

```sh
set -a
source ./.env
set +a
```

Spring Boot ne lit pas automatiquement le fichier `.env`. Ce chargement exporte les valeurs pour le processus lancé depuis ce terminal.

La propriété `sky.datasource.database` vaut `sky_take_out` par défaut dans `application-dev.yml`. Pour utiliser la base neuve du dépôt sans modifier le fichier de configuration, surchargez-la dans le même terminal :

```sh
export SKY_DATASOURCE_DATABASE=sky_take_out_demo
```

Avant de tester une commande immédiate, vérifiez que la boutique est ouverte depuis le back-office.

## 4. Construire et démarrer le backend

Depuis la racine, compilez et installez les modules Maven dans le dépôt local. L’installation des modules partagés permet ensuite au lancement depuis `sky-server` de résoudre `sky-common` et `sky-pojo` après un clone neuf :

```sh
mvn -pl sky-server -am install -DskipTests
```

Puis, toujours depuis la racine et dans le terminal où `.env` est chargé, lancez le profil local :

```sh
cd sky-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le serveur écoute sur `http://localhost:8080`. La page de documentation Knife4j est exposée à `http://localhost:8080/doc.html`. Le profil `dev` active le paiement simulé ; il ne configure pas un vrai moyen de paiement.

## 5. Lancer le back-office

Dans un autre terminal, depuis la racine :

```sh
cd admin-web
npm ci
npm run serve
```

Le serveur de développement écoute sur `http://localhost:8888`. La configuration `.env.development` définit `VUE_APP_URL=http://localhost:8080/admin` et le proxy Vue retire le préfixe `/api` avant de transmettre les requêtes. Le WebSocket utilise `ws://localhost:8080/ws/`.

| Identifiant | Mot de passe | Usage |
| --- | --- | --- |
| `admin` | `123456` | Démonstration locale uniquement |

Les pages de catalogue sont utilisables sans AppID WeChat ni clé AMap. Les images existantes sont des URL HTTPS publiques stockées dans les données ; leur affichage dépend de la disponibilité de ces URL. L’upload via `/admin/common/upload` nécessite `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` et `CLOUDINARY_API_SECRET`.

## 6. Compiler et ouvrir le mini-programme (optionnel)

### Outils et identifiants

Installez [HBuilderX depuis DCloud](https://www.dcloud.io/hbuilderx.html) et [WeChat DevTools depuis la page officielle WeChat](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html). La page DCloud propose des builds macOS Intel, macOS ARM et Windows. L’URL WeChat est le chemin officiel habituel, mais son ouverture n’a pas pu être vérifiée avec l’outil de consultation de cette session ; les systèmes et builds actuellement proposés restent donc à confirmer avant diffusion. Le projet est un uni-app visuel ; `mini-program/package.json` ne définit ni commande `build` ni CLI uni-app. HBuilderX est donc l’outil de compilation utilisé ici. `npm ci` dans `mini-program` installe les dépendances verrouillées pour les scripts statiques du dépôt, mais ne compile pas le mini-programme.

Créez ou utilisez un compte développeur de mini-programme sur [mp.weixin.qq.com](https://mp.weixin.qq.com/) et récupérez l’AppID et l’AppSecret dans ses réglages développeur. Un compte WeChat personnel ne fournit pas automatiquement ces identifiants. Configurez `WECHAT_APPID` et `WECHAT_SECRET` dans `.env`, puis rechargez le fichier dans le terminal backend. L’AppID utilisé à la compilation/import doit être celui associé au backend. Configurez localement l’AppID dans `mini-program/manifest.json` (`mp-weixin.appid`) et dans la configuration du projet WeChat utilisée par DevTools ; ne publiez pas d’AppSecret. Sans ces identifiants valides, le flux de connexion existant ne peut pas fonctionner.

### Compilation

1. Ouvrez le dossier `mini-program` dans HBuilderX.
2. Vérifiez que l’AppID WeChat du projet correspond à `WECHAT_APPID` côté backend.
3. Pour le développement, choisissez **Run > Run to Mini Program > WeChat DevTools**. HBuilderX affiche le chemin compilé dans sa console ; dans l’environnement de ce dépôt, la sortie de développement est `mini-program/unpackage/dist/dev/mp-weixin/`.
4. Vérifiez que le dossier de sortie contient `app.json`. N’importez pas le dossier source `mini-program` directement dans DevTools : `app.json` est généré lors de la compilation.
5. Pour une sortie de publication locale, HBuilderX documente **发行 > 小程序-微信** ; le chemin standard produit est `mini-program/unpackage/dist/build/mp-weixin/`. La sortie de développement et celle de publication sont distinctes.

La documentation uni-app décrit [l’exécution et la publication vers WeChat](https://uniapp.dcloud.net.cn/quickstart-hx.html). Dans WeChat DevTools, utilisez **Import project**, sélectionnez le dossier compilé qui contient `app.json`, puis vérifiez l’AppID et compilez.

`mini-program/utils/env.js` définit actuellement `baseUrl` à `http://localhost:8080`. Ce réglage convient au simulateur local de développement. Sur un téléphone physique, remplacez temporairement `localhost` par l’adresse IP LAN de la machine backend, et activez uniquement l’option de développement local de contournement de vérification de domaine dans DevTools. N’utilisez pas ce contournement pour une mise en ligne ; une version publiée exige un domaine HTTPS/WSS autorisé par la plateforme.

### Services nécessaires aux commandes

Les demandes de commande appellent le géocodage et le calcul d’itinéraire AMap avec les adresses originales. Une clé Web Service AMap est nécessaire pour le contrôle complet de livraison ; configurez-la dans `AMAP_KEY`. Le backend vérifie une distance d’itinéraire maximale de 5 km. Sans clé, le back-office de catalogue reste démontrable, mais le flux de commande avec validation de livraison ne l’est pas. La clé se crée depuis la [documentation officielle AMap Web Service](https://lbs.amap.com/api/webservice/summary). Le projet n’a pas d’abstraction de fournisseur cartographique ; changer de fournisseur nécessite une adaptation du code.

Le paiement fourni est une simulation activée par le profil `dev` ; aucune transaction réelle n’est nécessaire pour son parcours de démonstration. Cela ne représente pas une intégration de paiement de production.

## Dépannage rapide

| Symptôme | Vérification |
| --- | --- |
| Connexion MySQL refusée | Vérifiez MySQL, `localhost:3306`, `DB_PASSWORD`, puis confirmez que les scripts ont créé `sky_take_out_demo`. Vérifiez `SKY_DATASOURCE_DATABASE`. |
| Connexion Redis refusée | Vérifiez que Redis écoute sur `localhost:6379`, base `0`, et que `REDIS_PASSWORD` correspond à sa configuration. |
| Échec de démarrage JWT | Les deux variables doivent être présentes, distinctes et décoder en au moins 32 octets. Rechargez `.env` après l’avoir modifié. |
| Proxy API en erreur | Démarrez Spring Boot sur le port 8080 avant Vue et vérifiez `VUE_APP_URL` dans `admin-web/.env.development`. |
| `40013 invalid appid` ou échec de connexion WeChat | Vérifiez que l’AppID de compilation est celui de `WECHAT_APPID` et que l’AppSecret correspondant est configuré côté backend. |
| `app.json` introuvable dans DevTools | Compilez d’abord dans HBuilderX puis importez le dossier de sortie contenant `app.json`, pas `mini-program`. |
| Commande refusée lors du contrôle de livraison | Configurez une clé Web Service valide dans `AMAP_KEY`. Pour une commande immédiate, vérifiez que la boutique est ouverte depuis le back-office. |

## Vérification des commandes documentées

Les chemins, variables, ports, scripts npm et commandes Maven ci-dessus ont été vérifiés dans les fichiers du dépôt. La compilation Maven et le build de production Vue ont été exécutés ; les commandes d’import WeChat, les identifiants externes et le parcours réel sur téléphone nécessitent le compte et les outils locaux du développeur.
