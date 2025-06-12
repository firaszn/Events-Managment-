# 🐘 Guide de Migration MySQL vers PostgreSQL

## ✅ Étapes Complétées

### 1. **Installation PostgreSQL**
- Télécharger depuis : https://www.postgresql.org/download/windows/
- Port par défaut : `5432`
- Utilisateur : `postgres`

### 2. **Configuration Base de Données**
```sql
-- Créer la base de données
CREATE DATABASE userdb;

-- Créer un utilisateur (optionnel)
CREATE USER userapp WITH PASSWORD 'password123';
GRANT ALL PRIVILEGES ON DATABASE userdb TO userapp;
```

### 3. **Modifications du Code**

#### **pom.xml** ✅
- Remplacé `mysql-connector-j` par `postgresql`

#### **application.properties** ✅
- URL : `jdbc:postgresql://localhost:5432/userdb`
- Driver : `org.postgresql.Driver`
- Dialect : `PostgreSQLDialect`

#### **Entity User.java** ✅
- `GenerationType.AUTO` → `GenerationType.IDENTITY`
- Table `user` → `users` (évite les conflits avec les mots réservés PostgreSQL)

## 🚀 Prochaines Étapes

### 1. **Mettre à jour le mot de passe**
Dans `application.properties`, remplacez :
```properties
spring.datasource.password=your_postgres_password
```

### 2. **Démarrer l'application**
```bash
# Avec le profil PostgreSQL
mvn spring-boot:run -Dspring-boot.run.profiles=postgres

# Ou avec la configuration par défaut
mvn spring-boot:run
```

### 3. **Vérifier la migration**
- L'application créera automatiquement les tables
- Testez les endpoints d'authentification
- Vérifiez que les données sont bien persistées

## 🔧 Outils de Debug

### **Connexion directe à PostgreSQL**
```bash
psql -U postgres -h localhost -d userdb
```

### **Vérifier les tables créées**
```sql
\dt                    -- Lister les tables
\d users              -- Décrire la table users
SELECT * FROM users;  -- Voir les données
```

### **Logs de Debug**
Les logs SQL sont activés dans `application-postgres.properties`

## 🚨 Points d'Attention

1. **Mots réservés** : `user` est un mot réservé en PostgreSQL → utilisez `users`
2. **Séquences** : PostgreSQL utilise `IDENTITY` au lieu de `AUTO_INCREMENT`
3. **Types de données** : Certains types MySQL peuvent différer
4. **Performance** : PostgreSQL a des optimisations différentes

## 📊 Avantages de PostgreSQL

- ✅ Meilleure conformité SQL
- ✅ Support avancé des types de données (JSON, Arrays, etc.)
- ✅ Performances supérieures pour les requêtes complexes
- ✅ Extensibilité et plugins
- ✅ Meilleur support des transactions ACID
