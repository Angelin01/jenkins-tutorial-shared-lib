# jenkins-tutorial-shared-lib
Shared library for the Jenkins tutorial

# Vars

This lib contains the following vars:

## isMaster

Simple callable function that returns true if the branch is `master` or `main`:
```groovy
if(isMaster()) {
    println 'We are running on master!'
}
```

## sanitizeImageTag

Sanitizes an image tag, removing all illegal special characters and replacing them with a `-`:
```groovy
sanitizeImageTag('feat/my-branch#123') // Returns feat-my-branch-123
```

## kaniko

This global var cannot be called directly, instead one must use one of two functions:
* `kaniko.buildAndPush`: when you want the image to be pushed to a remote repository, like ECR.
* `kaniko.buildNoPush`: when you just want to build the image.

**This function requires a Kaniko container to be available on the pod.**

### Parameters

Both functions take a `Map args` argument. One can pass this map directly or use Groovy's syntax sugar to help, both
of these are equivalent:

```groovy
// This
def myMap = [foo: "value", bar: 123]
myFunc(myMap)

// Is equivalent to just this
myFunc foo: "value", bar: 123
```

Both functions receive the exact same parameters, described below. Parameters without a default value are required.
For reference, Full Docker image names use the following pattern: `REGISTRY/IMAGE_NAME:IMAGE_TAG`. Check [this
Stackoverflow question](https://stackoverflow.com/questions/37861791/how-are-docker-image-names-parsed) for a better
explanation.

| Parameter      | Default                          | Description                                                                                                                                                     |
|----------------|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `registry`     | -                                | The name of the Docker Image registry to use. Probably an ECR name.                                                                                             |
| `imageName`    | -                                | The name of the image to be built, without the tag or registry.                                                                                                 |
| `imageTag`     | -                                | The tag(s) to be used on the Docker image. Accepts either a string or a list of strings, if you wish to push multiple tags (like the commit but also `latest`). |
| `container`    | `'kaniko'`                       | The name of the Kaniko container.                                                                                                                               |
| `shell`        | `'/busybox/sh'`                  | Path of the shell inside the Kaniko container.                                                                                                                  |
| `dockerfile`   | `"${env.WORKSPACE}/Dockerfile"`  | Path of the Dockerfile, passed to Kaniko with the `-f` argument.                                                                                                |
| `context`      | `env.WORKSPACE`                  | Path of the docker build context, passed to Kaniko with the `-c` argument.                                                                                      |
| `extraArgs`    | `''`                             | Extra arguments to pass to Kaniko. See the [official documentation](https://github.com/GoogleContainerTools/kaniko#additional-flags) for help.                  |
| `pathOverride` | `"/kaniko:/busybox:${env.PATH}"` | Overrides the `PATH` environment variable.                                                                                                                      |

When using `kaniko.buildNoPush`, the `--no-push` argument is automatically passed to Kaniko, there's no need to provide
it using `extraArgs`.

### Examples:

#### Simple build without push
```groovy
stage('Kaniko Build') {
    when {
        expression { !isMaster() }
    }
    steps {
        script {
            kaniko.buildNoPush(
                    registry: '127793779807.dkr.ecr.us-east-1.amazonaws.com',
                    imageName: 'our-application',
                    imageTag: '1.2.3-alpine',
                    extraArgs: '--build-arg NPM_TOKEN=${NPM_TOKEN}'
            )
        }
    }
}
```

#### Re-utilizing code for multiple stages
```groovy
pipeline {
    // -- snip --
    stage('Kaniko Build | No Push') {
        when {
            expression { !isMaster() }
        }
        steps {
            script {
                kaniko.buildNoPush kanikoArgs()
            }
        }
    }
    stage('Kaniko Build And Push') {
        when {
            expression { isMaster() }
        }
        steps {
            script {
                kaniko.buildAndPush kanikoArgs()
            }
        }
    }
    // -- snip --
}

def kanikoArgs() {
    return [
            registry: '127793779807.dkr.ecr.us-east-1.amazonaws.com',
            imageName: 'our-application',
            imageTag: '1.2.3-alpine',
            dockerfile: "${env.WORKSPACE}/docker/Dockerfile",
            context: "${env.WORKSPACE}/docker",
            extraArgs: '--build-arg NPM_TOKEN=${NPM_TOKEN}'
    ]
}
```

#### Pushing multiple tags
```groovy
stage('Kaniko Build') {
    steps {
        script {
            kaniko.buildAndPush(
                    registry: '127793779807.dkr.ecr.us-east-1.amazonaws.com',
                    imageName: 'our-application',
                    imageTag: [env.GIT_COMMIT, 'latest']
            )
        }
    }
}
```
