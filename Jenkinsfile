def workerNode = 'devel10'
def dockerRepository = 'https://docker-de.artifacts.dbccloud.dk'

if (env.BRANCH_NAME == 'master') {
    properties([
        disableConcurrentBuilds(),
        pipelineTriggers([
            triggers: [
                [
                    $class: 'jenkins.triggers.ReverseBuildTrigger',
                    upstreamProjects: "Docker-payara5-bump-trigger, ../pg-queue/master, ../dbc-commons", threshold: hudson.model.Result.SUCCESS
                ]
            ]
        ]),
    ])
}
pipeline {
    agent { label workerNode }
    tools {
        maven "Maven 3"
        jdk 'jdk11'
    }
    environment {
        MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
        DOCKER_PUSH_TAG = "${env.BUILD_NUMBER}"
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
    }
    triggers {
        pollSCM("H/3 * * * *")
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
    }
    stages {
        stage("build") {
            steps {
                script {
                    def version = readMavenPom().version.replace('-SNAPSHOT', '')
                    def label = imageLabel()

                    def status = sh returnStatus: true, script:  """
                        rm -rf \$WORKSPACE/.repo
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo dependency:resolve dependency:resolve-plugins >/dev/null 2>&1 || true
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo clean
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo -Ddocker.extra.args="--pull" -Ddocker.image.version=${version} -Ddocker.image.label=${label} --fail-at-end org.jacoco:jacoco-maven-plugin:prepare-agent install -Dsurefire.useFile=false
                    """

                    // We want code-coverage and pmd/spotbugs even if unittests fails
                    status += sh returnStatus: true, script:  """
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo -pl !update-ws-transport pmd:pmd pmd:cpd spotbugs:spotbugs javadoc:aggregate -Dspotbugs.excludeFilterFile=src/test/spotbugs/spotbugs-exclude.xml
                    """

                    junit testResults: '**/target/*-reports/TEST-*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    def javadoc = scanForIssues tool: [$class: 'JavaDoc']
                    publishIssues issues:[java, javadoc]

                    def pmd = scanForIssues tool: [$class: 'Pmd'], pattern: '**/target/pmd.xml'
                    publishIssues issues:[pmd], unstableTotalAll:1

                    def cpd = scanForIssues tool: [$class: 'Cpd'], pattern: '**/target/cpd.xml'
                    publishIssues issues:[cpd]

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs'], pattern: '**/target/spotbugsXml.xml'
                    publishIssues issues:[spotbugs], unstableTotalAll:1

                    step([$class: 'JacocoPublisher',
                          execPattern: 'target/*.exec,**/target/*.exec',
                          classPattern: 'target/classes,**/target/classes',
                          sourcePattern: 'src/main/java,**/src/main/java',
                          exclusionPattern: 'src/test*,**/src/test*,**/*?Request.*,**/*?Response.*,**/*?Request$*,**/*?Response$*,**/*?DTO.*,**/*?DTO$*'
                    ])

                    warnings consoleParsers: [
                         [parserName: "Java Compiler (javac)"],
                         [parserName: "JavaDoc Tool"]],
                         unstableTotalAll: "0",
                         failedTotalAll: "0"

                    if ( status != 0 ) {
                        currentBuild.result = Result.FAILURE
                    }
                }
            }
        }

        stage('Docker') {
            steps {
                script {
                    def pom = readMavenPom()
                    def version = pom.version.replace('-SNAPSHOT', '')
                    def label = imageLabel()
                    if (currentBuild.resultIsBetterOrEqualTo('SUCCESS')) {
                        docker.withRegistry(dockerRepository, 'docker') {
                            for(def image : ["holdings-items-content-service", "holdings-items-kafka-bridge", "holdings-items-monitor", "holdings-items-postgres", "holdings-items-postgres-content", "holdings-items-purge-tool", "holdings-items-solr-indexer", "holdings-items-update-webservice"]) {
                                def app = docker.image("${image}-${version}:${label}")
                                app.push()
                                if (env.BRANCH_NAME == "master") {
                                    app.push "latest"
                                }
                            }
                        }
                    }
                }
            }
        }

        stage("upload") {
            steps {
                script {
                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                        sh """
                            mvn -Dmaven.repo.local=\$WORKSPACE/.repo -DskipTests -DskipITs package deploy:deploy
                        """
                    }
                }
            }
        }
        stage("Update DIT") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                expression {
                    (currentBuild.result == null || currentBuild.result == 'SUCCESS') && env.BRANCH_NAME == 'master'
                }
            }
            steps {
                script {
                    dir("deploy") {
                        sh "set-new-version databases/holdings-items-db.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets ${DOCKER_PUSH_TAG} -b master"
                        sh "set-new-version services/search/holdings-items-indexer.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets ${DOCKER_PUSH_TAG} -b master"
                        sh "set-new-version services/search/holdings-items-content-service.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets ${DOCKER_PUSH_TAG} -b master"
                        sh "set-new-version migrator/holdings-items-update-1-2.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets ${DOCKER_PUSH_TAG} -b master"
                    }
                }
            }
        }
    }

    post {
        fixed {
            script {
                if ("${env.BRANCH_NAME}" == 'master') {
                    emailext(
                            recipientProviders: [developers(), culprits()],
                            to: teamEmail,
                            subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} back to normal",
                            mimeType: 'text/html; charset=UTF-8',
                            body: "<p>The master is back to normal.</p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                            attachLog: false)
                    slackSend(channel: teamSlack,
                            color: 'good',
                            message: "${env.JOB_NAME} #${env.BUILD_NUMBER} back to normal: ${env.BUILD_URL}",
                            tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
        failure {
            script {
                if ("${env.BRANCH_NAME}" == 'master') {
                    emailext(
                            recipientProviders: [developers(), culprits()],
                            to: "de-team@dbc.dk",
                            subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} failed",
                            mimeType: 'text/html; charset=UTF-8',
                            body: "<p>The master build failed. Log attached. </p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                            attachLog: true,
                    )
                    slackSend(channel: 'de-notifications',
                            color: 'warning',
                            message: "${env.JOB_NAME} #${env.BUILD_NUMBER} failed and needs attention: ${env.BUILD_URL}",
                            tokenCredentialId: 'slack-global-integration-token')

                } else {
                    // this is some other branch, only send to developer
                    emailext(
                            recipientProviders: [developers()],
                            subject: "[Jenkins] ${env.BUILD_TAG} failed and needs your attention",
                            mimeType: 'text/html; charset=UTF-8',
                            body: "<p>${env.BUILD_TAG} failed and needs your attention. </p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                            attachLog: false,
                    )
                }
            }
        }
        success {
            step([$class: 'JavadocArchiver', javadocDir: 'target/site/apidocs', keepAll: false])
            archiveArtifacts artifacts: '**/target/*-jar-with-dependencies.jar', fingerprint: true
        }
    }
}

def imageLabel() {
    def label = env.BRANCH_NAME.toLowerCase()
    if (env.CHANGE_BRANCH) {
        label = env.CHANGE_BRANCH.toLowerCase()
    }
    if (label == "master") {
        label = env.BUILD_NUMBER
    } else {
        label = label.split(/\//)[-1]
    }
    return label
}
