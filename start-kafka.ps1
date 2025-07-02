# Définir les chemins
$KAFKA_HOME = "C:\kafka_2.13-3.6.1"
$KAFKA_CONFIG = "$KAFKA_HOME\config"
$KAFKA_LOGS = "$KAFKA_HOME\logs"
$ZOOKEEPER_DATA = "$KAFKA_HOME\data\zookeeper"

# Créer les répertoires nécessaires s'ils n'existent pas
New-Item -ItemType Directory -Force -Path $KAFKA_LOGS
New-Item -ItemType Directory -Force -Path $ZOOKEEPER_DATA

# Modifier les fichiers de configuration
$serverProperties = Get-Content "$KAFKA_CONFIG\server.properties"
$serverProperties = $serverProperties -replace "log.dirs=.*", "log.dirs=$($KAFKA_LOGS -replace '\\', '/')"
Set-Content "$KAFKA_CONFIG\server.properties" $serverProperties

$zookeeperProperties = Get-Content "$KAFKA_CONFIG\zookeeper.properties"
$zookeeperProperties = $zookeeperProperties -replace "dataDir=.*", "dataDir=$($ZOOKEEPER_DATA -replace '\\', '/')"
Set-Content "$KAFKA_CONFIG\zookeeper.properties" $zookeeperProperties

Write-Host "Configuration terminée. Démarrage des services..."

# Démarrer Zookeeper en arrière-plan
Start-Process -FilePath "C:\kafka_2.13-3.6.1\bin\windows\zookeeper-server-start.bat" -ArgumentList "C:\kafka_2.13-3.6.1\config\zookeeper.properties" -WindowStyle Hidden

# Attendre que Zookeeper soit complètement démarré
Start-Sleep -Seconds 10

# Démarrer Kafka en arrière-plan
Start-Process -FilePath "C:\kafka_2.13-3.6.1\bin\windows\kafka-server-start.bat" -ArgumentList "C:\kafka_2.13-3.6.1\config\server.properties" -WindowStyle Hidden 