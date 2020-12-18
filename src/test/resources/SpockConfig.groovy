import de.ii.xtraplatform.runtime.test.Slow

runner {
    if (!System.properties['spock.include.Slow']) {
        exclude {
            annotation Slow
        }
    }
}