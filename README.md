# jenkins_library

Offers tools to be used inside jenkins pipelines

[https://www.jenkins.io/doc/book/pipeline/shared-libraries/](jenkins doc)
[https://medium.com/@AndrzejRehmann/private-jenkins-shared-libraries-540abe7a0ab7](good tutorial)

> ⚠️ Important thing to know is that jenkins library run on master node

- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Release a new version](#release-a-new-version)
- [Linter formats](#linter-formats)
  - [build and run NodeLibrary docker image locally](#build-and-run-nodelibrary-docker-image-locally)
  - [Eslint](#eslint)
  - [Stylelint](#stylelint)
  - [NPM audit](#npm-audit)
  - [NPM outdated](#npm-outdated)

## Usage

```groovy
def lib = library(
    identifier: 'jenkins_library@v1.0.0',
    retriever: modernSCM([
        $class: 'GitSCMSource',
        remote: 'git@github.com:fchastanet/jenkins_library.git',
        credentialsId: 'babee6c1-14fe-4d90-9da0-ffa7068c69af'
    ])
)
def libDocker = lib.fchastanet.Docker.new(this)
def libGit = lib.fchastanet.Git.new(this)
def libLint = lib.fchastanet.Lint.new(this)
def libMail = lib.fchastanet.Mail.new(this)
def libUtils = lib.fchastanet.Utils.new(this)

```

## API Documentation

[API Documentation](doc/_index.md)

API documentation is auto generated from class comments using this command:

```bash
./generateDoc.sh
```

## Release a new version

in order to release a new version

- Update RELEASE_NOTES.md
- execute the following commands

```bash
# use --force in both command in order to overwrite the tag
git tag v2.0.0
git push --tags
```

## Linter formats

The `Lint` class proposes a method in order to publish results of the linters you run on your project.
This method reports the linter results using Warnings Next Generation Jenkins Plugin

- @see <https://jenkins.io/doc/pipeline/steps/warnings-ng/>
- @see <https://github.com/jenkinsci/warnings-ng-plugin/blob/master/doc/Documentation.md>
- @see <https://github.com/jenkinsci/warnings-ng-plugin/blob/master/SUPPORTED-FORMATS.md>

Here the documentation about how to format the results of your linter in order to be compatible with
this plugin

### build and run NodeLibrary docker image locally

Build image

```bash
docker build -t lint-converters src/fchastanet/warnings-ng
```

Launch image container to run a converter

```bash
npm audit --json | docker run -i --rm lint-converters jq -f npmAudit-v2.jq
```

### Eslint

Eslint config file example with vue js support `.eslintrc.js`

```js
module.exports = {
  root: true,
  env: {
    node: true
  },
  extends: [
    'plugin:vue/vue3-recommended',
    '@vue/standard',
    '@vue/typescript/recommended'
  ],
  parserOptions: {
    ecmaVersion: 2020
  },
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-process-env': 2
  },
  overrides: [
    {
      files: [
        '**/__tests__/*.{j,t}s?(x)',
        '**/tests/unit/**/*.spec.{j,t}s?(x)'
      ],
      env: {
        jest: true
      }
    }
  ]
}
```

.eslintignore

```bash
/.idea
/.vscode
/build
/dist
/lib
/node_modules
/packages
*.*
!*.js
!*.ts
!*.vue
```

npm packages needed:

```bash
npm i -D \
  eslint \
  eslint-plugin-import \
  eslint-plugin-node \
  eslint-plugin-promise \
  eslint-plugin-standard \
  eslint-plugin-vue \
  @typescript-eslint/parser \
  @typescript-eslint \
  plugin:vue/vue3-recommended \
  @vue/standard \
  @vue/typescript/recommended
```

Command to use in order to export eslint in checkstyle format

```bash
eslint --ext .js,.ts,.vue --ignore-path .gitignore -f checkstyle -o logs/eslint-checkstyle.xml .
```

Line to add in Jenkinsfile

```groovy
libLint.report(REFERENCE_JOB_NAME, [
  [
    tool: 'checkstyle', 
    pattern: 'logs/eslint-checkstyle.xml', 
    id: "esLint", 
    name: 'eslint', 
    qualityGates: [[threshold: 1, type: 'NEW', unstable: true]], 
    ignoreQualityGate: false
  ],
])
```

### Stylelint

Stylelint config file `.stylelintrc.js`

```js
module.exports = {
  extends: ['stylelint-config-standard', 'stylelint-config-idiomatic-order'],
  plugins: ['stylelint-scss']
}
```

npm packages needed:

```bash
npm i -D stylelint
npm i -D stylelint-config-standard
npm i -D stylelint-config-idiomatic-order
npm i -D stylelint-scss
npm i -D stylelint-checkstyle-formatter
```

We are using `stylelint-checkstyle-formatter` in order to convert stylelint to checkstyle format

Command to use in order to export stylelint in checkstyle format

```bash
./node_modules/.bin/stylelint -f json -o logs/stylelint.json 'src/**/*.{css,scss,vue}'
```

Line to add in Jenkinsfile

```groovy
libLint.transformReport('./logs/stylelint.json', './logs/ng-stylelint.json', 'fchastanet.transformers.EslintJsonToStylelint'
libLint.report(REFERENCE_JOB_NAME, [
  [
    tool: 'issues', 
    pattern: 'logs/ng-stylelint.json', 
    id: "stylelint",
    name: 'stylelint', 
    qualityGates: [[threshold: 1, type: 'NEW', unstable: true]], 
    ignoreQualityGate: false
  ],
])
```

### NPM audit

Command to use in order to export npm audit in json format and convert to waning NG issues format

```bash
npm audit --json | docker run -i --rm lint-converters jq -f npmAudit-v2.jq > logs/npm-audit.json
```

Line to add in Jenkinsfile

```groovy
libLint.report(REFERENCE_JOB_NAME, [
  [
    tool: 'issues', 
    pattern: 'logs/npm-audit.json', 
    id: "npmAudit", 
    name: 'npm audit', 
    qualityGates: [[threshold: 1, type: 'NEW', unstable: true]], ignoreQualityGate: false],
    ignoreQualityGate: false
  ],
])
```

### NPM outdated

Command to use in order to export npm audit in json format and convert to waning NG issues format

```bash
npm outdated --json | docker run -i --rm lint-converters jq -f npmOutdated-v1.jq > logs/npm-outdated.json
```

Line to add in Jenkinsfile

```groovy
libLint.report(REFERENCE_JOB_NAME, [
  [
    tool: 'issues', 
    pattern: 'logs/npm-outdated.json', 
    id: "npmOutdated", 
    name: 'npm outdated', 
    qualityGates: [[threshold: 1, type: 'NEW', unstable: true]], 
    ignoreQualityGate: false
  ],
])
```
