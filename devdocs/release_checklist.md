# Release checklist

This release checklist helps during the preparation and deployment of releases.
It gives instructions for use to bump a version number, built the project, create proper branches and upload artifacts 
to Maven Central.

## Preparation

### 1. Check dependencies for updates

Walk through all Maven project dependencies and update to the latest version.
Ship your changes to develop by means of a merge request.

### 2. Setup release branch

Create a new release branch from latest develop named `release/X.X` with `X.X` being the major and minor version of the
release.

> Note that bug fix versions are not part of the release branch name as those are supposed to be merged into the release 
branch without changing the branch name.

### 3. Double-check release notes for completeness

Typically the changelog is supposed to be fostered during development on develop.
Nevertheless, a final look to the changelog can help finding inconsistencies with the actual implementation.
Once done, bump the version number to the CHANGELOG.md file by moving the _[Unreleased]_ contents to a new section 
labelled with the upcoming version and release date.

Example: 

```
## [Unreleased]
    
### Added

- Foo
- Bar
```

is changed to
    
```
## [Unreleased]
    
## [2.2.1] - 2020-02-29
    
### Added
        
- Foo
- Bar
```

### 4. Call Maven's release preparation to bump version numbers on the parent module and all submodules

```
mvn release:prepare
```

Always type in the three-fielded semantic version number when asked.
The requested Git tag name shall have the format `sdc-ri-X.X.X` with `X.X.X` being the version number of the release.

## Deployment

### 5. Call Maven's project deployment command

```
mvn -DskipTests=true install deploy -Pdeploy 
```

To deploy the project, Maven requires the signing process to be configured in the local _.m2/settings.xml_.
See the following template:

```xml
<servers>
    <server>
        <id>ossrh</id>
        <username>SONATYPE_USERNAME</username>
        <password>SONATYPE_PASSWORD</password>
    </server>
</servers>
<profiles>
    <profile>
        <id>ossrh</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <!-- note the exclamation mark at the end, which is intentional -->
            <gpg.keyname>GPG_KEYNAME!</gpg.keyname>
            <gpg.passphrase>GPG_PASSPHRASE</gpg.passphrase>
        </properties>
    </profile>
</profiles>
```

### 6. Final verification prior to Maven Central deployment

As an additional verification step before delivering to Maven Central, go to http://oss.sonatype.org, login and click
 _Staging Repositories_ from the left menu. 
Double-check that everything is as expected, _Close_ the staged artifacts and _Release_ them.

### 7. Fast-forward to master and push

Assuming you are still in the release branch.
Push your changes to the Gitlab repository by calling:

```
git push origin release/X.X
```

Fast-forward changes to master and push by calling:

```
git checkout master
git merge release/X.X
git push origin master
```

Do not forget to push the locally generated version tag:

````
git push origin sdc-ri-X.X.X
````

> In order to successfully conduct the push commands you need Gitlab _Maintainer_ rights.

### 8. Publish release description

Add a Gitlab release description that includes the changelog of the preceding release.
Feel free to add further notes if they are of any public interest.
See https://gitlab.com/help/user/project/releases/index to find more information on Gitlab release descriptions.