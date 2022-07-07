def call(String tag) {
    return tag.replaceAll(/[^a-zA-Z0-9_\-\.]/, '-')
}
