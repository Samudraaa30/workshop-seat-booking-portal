pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds() }
  parameters {
    booleanParam(name: 'PUBLISH_IMAGE', defaultValue: false, description: 'Push the built image to Docker Hub (main branch only).')
    booleanParam(name: 'DEPLOY', defaultValue: false, description: 'Deploy to the configured SSH host with Ansible (main branch only).')
    string(name: 'DEPLOY_HOST', defaultValue: '', description: 'SSH hostname or IP for an Ansible deployment.')
    string(name: 'DEPLOY_SSH_USER', defaultValue: 'ubuntu', description: 'SSH user on the deployment host.')
  }
  environment {
    IMAGE_NAME = 'workshop-seat-booking'
    DOCKERHUB_NAMESPACE = 'SET_DOCKERHUB_NAMESPACE'
    REGISTRY_CREDENTIALS = 'dockerhub-token'
    DATABASE_CREDENTIALS = 'workshop-production-db'
    DEPLOY_KEY = 'workshop-deploy-ssh-key'
  }
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Build, tests and package') {
      steps {
        withEnv(["JAVA_HOME=${tool 'jdk-21'}", "PATH+JAVA=${tool 'jdk-21'}/bin:${tool 'maven-3.9'}/bin"]) {
          sh 'mvn -B clean verify'
        }
      }
    }
    stage('Archive package') {
      steps { archiveArtifacts artifacts: 'target/workshop-seat-booking-*.jar', fingerprint: true }
    }
    stage('Build Docker image') {
      steps { sh 'docker build --pull -t ${IMAGE_NAME}:${BUILD_NUMBER} -t ${IMAGE_NAME}:latest .' }
    }
    stage('Publish image') {
      when { expression { return params.PUBLISH_IMAGE } }
      steps {
        script {
          def branchName = env.BRANCH_NAME ?: (env.GIT_BRANCH ?: '').replaceFirst('^origin/', '')
          if (branchName != 'main') error('Image publishing is allowed only from main.')
        }
        withCredentials([usernamePassword(credentialsId: env.REGISTRY_CREDENTIALS, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_TOKEN')]) {
          sh '''
            trap 'docker logout >/dev/null 2>&1 || true' EXIT
            echo "$REGISTRY_TOKEN" | docker login -u "$REGISTRY_USER" --password-stdin
            docker tag "$IMAGE_NAME:$BUILD_NUMBER" "$DOCKERHUB_NAMESPACE/$IMAGE_NAME:$BUILD_NUMBER"
            docker tag "$IMAGE_NAME:$BUILD_NUMBER" "$DOCKERHUB_NAMESPACE/$IMAGE_NAME:latest"
            docker push "$DOCKERHUB_NAMESPACE/$IMAGE_NAME:$BUILD_NUMBER"
            docker push "$DOCKERHUB_NAMESPACE/$IMAGE_NAME:latest"
          '''
        }
      }
    }
    stage('Deploy with Ansible') {
      when { expression { return params.DEPLOY } }
      steps {
        script {
          def branchName = env.BRANCH_NAME ?: (env.GIT_BRANCH ?: '').replaceFirst('^origin/', '')
          if (branchName != 'main') error('Deployment is allowed only from main.')
          if (!params.DEPLOY_HOST?.trim()) error('Set DEPLOY_HOST before enabling deployment.')
          if (!params.PUBLISH_IMAGE) error('Enable PUBLISH_IMAGE before deployment.')
          if (env.DOCKERHUB_NAMESPACE == 'SET_DOCKERHUB_NAMESPACE') error('Set DOCKERHUB_NAMESPACE in the Jenkinsfile.')
        }
        withCredentials([
          usernamePassword(credentialsId: env.DATABASE_CREDENTIALS, usernameVariable: 'SPRING_DATASOURCE_USERNAME', passwordVariable: 'SPRING_DATASOURCE_PASSWORD'),
          string(credentialsId: 'workshop-production-jdbc-url', variable: 'SPRING_DATASOURCE_URL'),
          usernamePassword(credentialsId: env.REGISTRY_CREDENTIALS, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_TOKEN')
        ]) {
          sshagent(credentials: [env.DEPLOY_KEY]) {
            sh '''
              ANSIBLE_HOST_KEY_CHECKING=True ansible-playbook -i "$DEPLOY_HOST," -u "$DEPLOY_SSH_USER" \\
                ansible/deploy.yml \\
                -e "portal_image=$DOCKERHUB_NAMESPACE/$IMAGE_NAME:$BUILD_NUMBER"
            '''
          }
        }
      }
    }
  }
  post {
    always { junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false }
    success { echo 'Build and tests passed; deployment runs only when explicitly enabled.' }
    failure { echo 'Pipeline failed. Review Maven, Docker, or Ansible logs before retrying.' }
  }
}
