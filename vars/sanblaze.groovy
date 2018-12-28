/*
 * sanblaze.groovy
 *
 * A collection of utility functions is defined below, intended to be used by Jenkins pipelines
 *
 * These routines describe the API for controlling SANBlaze SBExpress test system from Jenkins
 * The library exists on github at:
 *
 *      https://github.com/asbridge/sanblaze-jenkins
 *
 *      To clone a copy to your local machine, do:
 *          git clone https://github.com/asbridge/sanblaze-jenkins.git
 *
 *      To add changed to the repository, do:
 *          git commit filename
 *          git push
 *
 *      To add to your Jenkins Pipeline environment, see RunningSANBlazeFromJenkins.docx, but basically
 *          - jenkins/manage/configure system
 *          - Global Pipeline Libraries
 *        	    Name SANBlazeAPI
 *           	Default version	master
 *              Retrieval method Modern SCM
 *              Source Code Management Git	
 *              Project Repository https://github.com/asbridge/sanblaze-jenkins.git
 *              Credentials	asbridge (contact sanblaze for password)
 *
 *          - In your pipeline script, include the library like this: (This line can be the first line in your script, before the "pipeline {" statement
 *              @Library('SANBlazeAPI') _
 *
 *          - Use the routines below in your pipeline script by referring to:
 *              sanblaze.function()
 *              Example: sanblaze.SayGreetings("Hello", "World!")
 */

 /*
  * Test routine to prove Library functionality
  */
def sayGreeting(greeting1, greeting2) {
    echo greeting1 + ' ' + greeting2
    return 0
}

/*
 * scriptAction - Perform the given action on a script that has been staged on the SBExpress system, check for requestedscriptstate
 */
def scriptAction(ip, s, p, t, l, T, action, requestedscriptstate){                    
    def start = httpRequest (consoleLogResponseBody: true, 
        contentType: 'APPLICATION_JSON', 
        httpMode: 'POST', 
        requestBody: action,
        url: "http://" + ip + "/goform/JsonApi?op=rest/sanblazes/" + s + "/ports/" + p + "/targets/" + t + "/luns/" + l + "/tests/" + T + "/requestedscriptstate", 
        validResponseCodes: '200')
//    writeFile file: 'start.txt', text: start.content
    println('Status: ' + start.status)
    println('Response: ' + start.content)
//    SayGreetings("Hello", "World from a subroutine")
    def props = readJSON text: start.content
    echo props[0].requestedscriptstate
    if (props[0].requestedscriptstate == requestedscriptstate){
        echo "Sucess! requestedscriptstate=" + requestedscriptstate
    } else {
        echo "Failure! requestedscriptstate=" + props[0].requestedscriptstate + " expected=" + requestedscriptstate
    }
}

/*
 * waitForState -   Given a specific test, wait for the test to reach expectedstates (one at the moment)
 *                  Returns zero if the state is reached before timeout seconds, or one if the state is not reached
 */
def waitForState(ip, s, p, t, l, T, expectedstates, timeout){
    def waited = 0;
    while (waited <= timeout){
        echo "Waiting for state " + expectedstates
        def checkstate = httpRequest (consoleLogResponseBody: true, 
            contentType: 'APPLICATION_JSON', 
            httpMode: 'GET', 
            url: "http://" + ip + "/goform/JsonApi?op=rest/sanblazes/" + s + "/ports/" + p + "/targets/" + t + "/luns/" + l + "/tests/" + T + "/scriptstate", 
            validResponseCodes: '200')
    //    writeFile file: 'start.txt', text: start.content
        println('getStatus: ' + checkstate.status)
        println('getResponse: ' + checkstate.content)
    //    SayGreetings("Hello", "World from a subroutine")
        def props1 = readJSON text: checkstate.content
        echo props1[0].scriptstate
        // this needs to walk a list of expectedstates
        echo "scriptstate=" + props1[0].scriptstate
        if (props1[0].scriptstate == expectedstates){
            echo "Sucess! requestedscriptstate=" + expectedstates
            return 0
        } else {
            echo "Failure! requestedscriptstate=" + props1[0].scriptstate + " expected=" + expectedstates
            sleep(1)
            waited++
        }
    }
    // If we get here, we failed
    currentBuild.result = "FAILURE"
    sh "exit 1"
}
