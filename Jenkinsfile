def dockerRepository = 'https://docker-os.dbc.dk'
if (env.BRANCH_NAME == 'master') {
    properties([
        disableConcurrentBuilds(),
        pipelineTriggers([
            triggers: [
                [
                    $class: 'jenkins.triggers.ReverseBuildTrigger',
                    upstreamProjects: "../pg-queue/master, ../ee-stats, ../dbc-commons", threshold: hudson.model.Result.SUCCESS
                ]
            ]
        ]),
    ])
}
pipeline {
    agent { label "devel8" }
    tools {
        maven "maven 3.5"
    }
    environment {
        MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
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
                // Fail Early..
                script {
                    if (! env.BRANCH_NAME) {
                        currentBuild.rawBuild.result = Result.ABORTED
                        throw new hudson.AbortException('Job Started from non MultiBranch Build')
                    } else {
                        println(" Building BRANCH_NAME == ${BRANCH_NAME}")
                    }
                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                        sh """
                            mvn -B clean
                            mvn -B deploy pmd:pmd javadoc:aggregate
                        """
                    } else {
                        sh """
                            mvn -B clean
                            mvn -B verify pmd:pmd javadoc:aggregate
                        """
                    }
                }

                //junit "**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml"
            }
        }

        stage('Docker') {
            steps {
                script {
                    def allDockerFiles = findFiles glob: '**/Dockerfile'
                    def dockerFiles = allDockerFiles.findAll { f -> f.path.endsWith("target/docker/Dockerfile") }
                    def version = readMavenPom().version.replace('-SNAPSHOT', '')

                    for (def f : dockerFiles) {
                        def dirName = f.path.take(f.path.length() - "target/docker/Dockerfile".length())
                        if ( dirName == '' )
                            dirName = '.'
                        dir(dirName) {
                            modulePom = readMavenPom file: 'pom.xml'
                            def projectArtifactId = modulePom.getArtifactId()
                            def imageName = "${projectArtifactId}-${version}".toLowerCase()
                            if (! env.CHANGE_BRANCH) {
                                imageLabel = env.BRANCH_NAME.toLowerCase()
                            } else {
                                imageLabel = env.CHANGE_BRANCH.toLowerCase()
                            }
                            if ( ! (imageLabel ==~ /master|trunk/) ) {
                                println("Using branch_name ${imageLabel}")
                                imageLabel = imageLabel.split(/\//)[-1]
                            } else {
                                println(" Using Master branch ${BRANCH_NAME}")
                                imageLabel = env.BUILD_NUMBER
                            }

                            println("In ${dirName} build ${projectArtifactId} as ${imageName}:$imageLabel")

                            def app = docker.build("$imageName:${imageLabel}", '--pull --no-cache --file target/docker/Dockerfile .')

                            if (currentBuild.resultIsBetterOrEqualTo('SUCCESS')) {
                                docker.withRegistry(dockerRepository, 'docker') {
                                    app.push()
                                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                                        app.push "latest"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
