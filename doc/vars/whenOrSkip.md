# Documentation

## `void call(boolean condition, Closure body=null)`

execute body or skip the stage if condition fails 

```Jenkinsfile
 stage('Zero') {
   whenOrSkip(BRANCH_NAME == 'master') {
     echo 'Performing steps if branch is master'
   }
 }
```

