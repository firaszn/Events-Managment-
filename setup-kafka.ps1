# Définir le chemin de base de Kafka
$KAFKA_HOME = "C:\kafka_2.13-3.6.1"

# Créer les dossiers nécessaires
Write-Host "Création des dossiers..."
New-Item -ItemType Directory -Force -Path "$KAFKA_HOME\logs"
New-Item -ItemType Directory -Force -Path "$KAFKA_HOME\data\zookeeper"

# Modifier le fichier server.properties
Write-Host "Configuration de Kafka..."
$serverPropertiesPath = "$KAFKA_HOME\config\server.properties"
$serverProperties = Get-Content $serverPropertiesPath
$serverProperties = $serverProperties -replace "log.dirs=.*", "log.dirs=$KAFKA_HOME\logs"
$serverProperties | Set-Content $serverPropertiesPath

# Modifier le fichier zookeeper.properties
Write-Host "Configuration de Zookeeper..."
$zookeeperPropertiesPath = "$KAFKA_HOME\config\zookeeper.properties"
$zookeeperProperties = Get-Content $zookeeperPropertiesPath
$zookeeperProperties = $zookeeperProperties -replace "dataDir=.*", "dataDir=$KAFKA_HOME\data\zookeeper"
$zookeeperProperties | Set-Content $zookeeperPropertiesPath

Write-Host "Configuration terminée!"
Write-Host "Pour démarrer Zookeeper, exécutez : .\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties"
Write-Host "Pour démarrer Kafka, exécutez : .\bin\windows\kafka-server-start.bat .\config\server.properties" 