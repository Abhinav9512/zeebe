#!/usr/bin/env groovy

// https://github.com/jenkinsci/pipeline-model-definition-plugin/wiki/Getting-Started

def commitId() {
  if (!env.COMMIT_ID) {
    script {
      sh 'git rev-parse --short=7 --verify HEAD | tr -d "\n" > GIT_COMMIT'
      return env.COMMIT_ID = readFile('GIT_COMMIT')
    }
  }
}



pipeline {
  agent { label 'optimize-build' }

  // Environment
  environment {
    DISPLAY = ":0"
  }

  options {
    // General Jenkins job properties
    buildDiscarder(logRotator(numToKeepStr:'10'))
    // "wrapper" steps that should wrap the entire build execution
    timestamps()
    timeout(time: 10, unit: 'MINUTES')
  }

  stages {
    stage('Prepare') {
      steps {
        configFileProvider([
          configFile(fileId: 'camunda-maven-settings', replaceTokens: true, targetLocation: 'settings.xml')
        ]) {}
      }
    }
    stage('Unit') {
      steps {
        sh '''\
            cd client
            yarn
            '''
        sh 'mvn -s settings.xml clean package'
      }
      post {
        always {
          junit testResults: '**/surefire-reports/**/*.xml', allowEmptyResults: true, healthScaleFactor: 1.0, keepLongStdio: true
        }
      }
    }
    stage('IT') {
      steps {
        sh 'mvn -s settings.xml -f optimize-backend/pom.xml clean verify'
      }
      post {
        always {
          junit testResults: '**/failsafe-reports/**/*.xml', allowEmptyResults: true, healthScaleFactor: 1.0, keepLongStdio: true
        }
        failure {
          archiveArtifacts artifacts: 'optimize-backend/target/elasticsearch*/logs/*.log', onlyIfSuccessful: true
        }
      }

    }
    stage('Docs') {
      steps {
        sh 'mvn -s settings.xml -f optimize-backend/pom.xml -DskipTests -Pdocs clean package'
      }
      post {
        success {
          archiveArtifacts artifacts: 'optimize-backend/target/docs/**/*.*', onlyIfSuccessful: true
        }
      }
    }
  }

  post {
    changed {
      emailext subject: "[Jenkins-Optimize] - Status[${currentBuild.rawBuild.result}] - ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
          body: """\
${env.JOB_NAME} - Build # ${env.BUILD_NUMBER}:
Status: ${currentBuild.rawBuild.result}
Check console output at ${env.BUILD_URL} to view the results.
""",
          recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]
    }
  }
}
