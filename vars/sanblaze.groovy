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
        echo "Success! requestedscriptstate=" + requestedscriptstate
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
            echo "Success! requestedscriptstate=" + expectedstates
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

/*
 * stageTest - Given a test name, and system, port, controller, namespace, assigns the test to the device
 *             Returns zero for success and one for failure
 *             Parameters:
 *                  ip - ip address of target system
 *                   s - system number of the SBExpress system
 *                   p - port number of the SBExpress system NVMe port
 *                   t - controller number for the target
 *                   l - namespace on the specified controller
 *               index - run sequence order for test, use -1 for auto assign
 *                name - name of the test, including one parent directory (example: NVMe_Generic/NVMe_AsyncEventRequest.sh)
 *              passes - number of times to run the test (default is one)
 *           passtimer - time to run each pass (note: only for tests that are not "one shot", such as "Inquiry")
 */
def stageTest(ip, s, p, t, l, index, name, passes, passtimer){
    echo "running stageTest " + name
    def checkstate = httpRequest (consoleLogResponseBody: true, 
        contentType: 'APPLICATION_JSON', 
        httpMode: 'POST', 
        url: "http://" + ip + "/goform/TestManagerAdd",
        requestBody: '-d express=1&ok=Add&system=' + s + '&port=' + p + '&target=' + t + '&lun=' + l + '&index=' + index + '&passes=' + passes + '&passtimer=' + passtimer + '&script=' + name + '', 
        validResponseCodes: '200')
    println('getStatus: ' + checkstate.status)
    println('getResponse: ' + checkstate.content)
    return 0
}
