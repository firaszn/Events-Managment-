pipeline {
    agent any

    environment {
        MAVEN_HOME = tool 'Maven'
        REGISTRY = "docker.io/firaszn"
        IMAGE_NAME = "events-management:${env.BUILD_NUMBER}"
        GITHUB_REPO = "https://github.com/firaszn/Events-Managment-.git"
        SERVICES = "api-gateway config-server eureka-server user-service event-service frontend invitation-service notification-service"

    }

    stages {
        stage('GIT Checkout') {
            steps {
                // Checkout du repo GitHub
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
                    userRemoteConfigs: [[
                        url: "${GITHUB_REPO}",
                        credentialsId: 'github-credentials' // À configurer dans Jenkins
                    ]]
                ])
            }
        }

        stage('Maven Clean') {
            steps {
                sh "${MAVEN_HOME}/bin/mvn clean"
            }
        }

        stage('Maven Compile') {
            steps {
                sh "${MAVEN_HOME}/bin/mvn compile"
            }
        }
          stage('Maven Test') {
            steps {
                sh "${MAVEN_HOME}/bin/mvn test"
            }
        }
        stage('Maven Package') {
         steps {
  sh "${MAVEN_HOME}/bin/mvn clean package -DskipTests"    }
}

      stage('SonarQube Analysis') {
    steps {
        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
            sh """
                ${MAVEN_HOME}/bin/mvn sonar:sonar \
                    -Dsonar.projectKey=events-management \
                    -Dsonar.host.url=http://localhost:9000 \
                    -Dsonar.login=$SONAR_TOKEN
            """
        }
    }
}

      stage('Nexus Deploy') {
    steps {
        withCredentials([usernamePassword(credentialsId: 'nexus-credentials', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
    sh "${MAVEN_HOME}/bin/mvn deploy -Dmaven.repo.local=${WORKSPACE}/.m2/repository -DskipTests"        }
    }
}

      stage('Docker Build') {
            steps {
                script {
                    def services = env.SERVICES.split()
                    services.each { svc ->
                        sh "docker build -t ${REGISTRY}/${svc}:${env.BUILD_NUMBER} ./${svc}"
                    }
                }
            }
        }

        stage('Docker Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                        def services = env.SERVICES.split()
                        services.each { svc ->
                            sh "docker push ${REGISTRY}/${svc}:${env.BUILD_NUMBER}"
                            if (env.BRANCH_NAME == 'main') {
                                sh "docker tag ${REGISTRY}/${svc}:${env.BUILD_NUMBER} ${REGISTRY}/${svc}:latest"
                                sh "docker push ${REGISTRY}/${svc}:latest"
                            }
                        }
                    }
                }
            }
        }
    }
} 