def call() {
    return env.GIT_BRANCH == 'master' || env.GIT_BRANCH == 'main'
}
