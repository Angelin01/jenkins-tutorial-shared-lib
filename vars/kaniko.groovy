def call() {
    error '''You can't call this var directly'''
}

def buildAndPush(Map args) {
    build(args)
}

def buildNoPush(Map args) {
    if (args.containsKey('extraArgs') && !args.extraArgs.contains('--no-push')) {
        args.extraArgs += ' --no-push'
    } else {
        args.extraArgs = '--no-push'
    }

    build(args)
}

private def build(Map args) {
    def destinations = destinationsFromArgs(args.registry, args.imageName, args.imageTag)
    def containerName = args?.container ?: 'kaniko'
    def shell = args?.shell ?: '/busybox/sh'
    def dockerfile = args?.dockerfile ?: "${env.WORKSPACE}/Dockerfile"
    def context = args?.context ?: env.WORKSPACE
    def extraArgs = args?.extraArgs ?: ''
    def path = args?.pathOverride ?: "/kaniko:/busybox:${env.PATH}"

    withEnv(["PATH=$path"]) {
        container(name: containerName, shell: shell) {
            sh """#!$shell
                executor -f "$dockerfile" -c "$context" $destinations $extraArgs
            """
        }
    }
}

private String destinationsFromArgs(String registry, String imageName, def imageTag) {
    if (!(imageTag instanceof List)) {
        imageTag = [imageTag]
    }

    return imageTag.collect { tag -> """--destination="${registry}/${imageName}:${tag}\"""" }.join(' ')
}
