#AMUtil

## Installing

#### Add jitpack repository to top (project) build.gradle file:

    maven { url 'https://jitpack.io' }

    Example:
    allprojects {
        repositories {
            jcenter()
            maven { url 'https://jitpack.io' }
        }
    }

#### Add dependency to app (module) build.gradle file:

    compile 'com.github.adaptmobile-organization:amexoplayer:version'

  Example:
    
    dependencies {
        compile 'com.github.adaptmobile-organization:amexoplayer:1.0.1'
    }

## Development

  Push a new/the next tag for a new version of amexoplayer on jitpack

## Jitpack

  https://jitpack.io/#adaptmobile-organization/amutil
