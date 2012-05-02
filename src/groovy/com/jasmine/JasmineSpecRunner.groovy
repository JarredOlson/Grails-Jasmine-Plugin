package com.jasmine

import groovy.util.slurpersupport.NodeChild
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

class JasmineSpecRunner {

	def static final FILE_TAG = 'file:///'	
	def timeoutInSeconds = 5
	
	def getHeader() {
		[
			'<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"',
			'http://www.w3.org/TR/html4/loose.dtd">',
			'<html>',
			'<head>',
			'<title>Jasmine Spec Runner</title>'
		]
	}
	
	def getJasmineFiles() {
		[
			'<link rel="shortcut icon" type="image/png" href="' + FILE_TAG + pluginRootDirectory + 'web-app/images/jasmine/jasmine_favicon.png" />',
			'<link rel="stylesheet" type="text/css" href="' + FILE_TAG + pluginRootDirectory + 'web-app/css/jasmine/jasmine.css" />',
			'<script type="text/javascript" src="' + FILE_TAG + pluginRootDirectory + 'web-app/js/jasmine/jasmine.js"></script>',
			'<script type="text/javascript" src="' + FILE_TAG + pluginRootDirectory + 'web-app/js/jasmine/jasmine-html.js"></script>'
		]
	}
	
	def getJasmineExecutorScript() {
		[
			'<script type="text/javascript">',
			'(function() {',
			'var jasmineEnv = jasmine.getEnv();',
			'jasmineEnv.updateInterval = 1000;',
			'var trivialReporter = new jasmine.TrivialReporter();',
			'jasmineEnv.addReporter(trivialReporter);',
			'jasmineEnv.specFilter = function(spec) {',
			'return trivialReporter.specFilter(spec);',
			'};',
			'var currentWindowOnload = window.onload;',
			'window.onload = function() {',
			'if (currentWindowOnload) {',
			'currentWindowOnload();',
			'}',
			'execJasmine();',
			'};',
			'function execJasmine() {',
			'jasmineEnv.execute();',
			'}',
			'})();',
			'</script>',
		]
	}

	def getFooter() {
		[
			'</head>',
			'<body>',
			'</body>',
			'</html>'
		]
	}
	
	def executeTests() {
		def file = createSpecFileAtLocation(getFilePath())
		def driver = new HtmlUnitDriver(true)
		driver.get("${FILE_TAG}${file.getAbsolutePath()}")
		def finished = false
		def loops = 0
		def slurper = new XmlSlurper()
		def htmlNodes
		while (loops < timeoutInSeconds && !finished) {
			htmlNodes = slurper.parseText(driver.getPageSource())
			finished = htmlNodes.'**'.find{ it.@class == "finished-at"}.text().trim() != ""
			if(!finished) {
				Thread.sleep(1000)
				loops++
			}
		}
		if(!finished) {
			throw new RuntimeException("\n\nSpecs did not complete within the specified time-out of ${timeoutInSeconds} seconds.\n")
		}
		def numberOfFailures = getNumberOfFailures(htmlNodes)
		if (0 != numberOfFailures) {
			printFailures(htmlNodes, numberOfFailures)
		}
	}
	
	def getNumberOfFailures(htmlNodes) {
		def splitResults = htmlNodes.'**'.find{ it.@class == "description"}.text().trim().split(",")
		String str = splitResults[1]
		str.substring(0, str.indexOf("failure")).trim() as int
	}
	
	private def printFailures(htmlNodes, numberOfFailures) {
		def errorNodes = htmlNodes.'**'.findAll{ it.@class == "spec failed"}
		def stringBuffer = new StringBuffer()
		stringBuffer.append("\n\n${numberOfFailures} Jasmine spec${numberOfFailures > 1 ? 's' : ''} failed.\n")
		errorNodes.each {node ->
			def specName = node.'**'.find{ it.@class == "description"}.attributes().title
			def failureMessage = node.'**'.find{ it.@class == "resultMessage fail"}.text().trim()
			stringBuffer.append("\n\n${specName}:\n${failureMessage}}")
		}
		stringBuffer.append("\n")
		throw new RuntimeException(stringBuffer.toString())
	}
	
	def createSpecFileAtLocation(givenFilePath) {
		def file = new File(givenFilePath)
		file.write(fileContentsAsString())
		file
	}
	
	def String fileContentsAsString() {
		def stringBuilder = new StringBuilder()
		appendAll(stringBuilder, header)
		
		stringBuilder.append("\n\n <!-- JASMINE FILES -->\n")
		appendAll(stringBuilder, jasmineFiles)
		
		stringBuilder.append("\n\n <!-- SOURCE FILES -->\n")
		appendAll(stringBuilder, sourceTags)
		
		stringBuilder.append("\n\n <!-- TEST FILES -->\n")
		appendAll(stringBuilder, testTags)
		
		stringBuilder.append("\n\n <!-- JASMINE EXECUTOR-->\n")
		appendAll(stringBuilder, jasmineExecutorScript)
		
		appendAll(stringBuilder, footer)
		
		stringBuilder.toString()
	}
	
	def appendAll(StringBuilder stringBuilder, lines) {
		lines.each {
			stringBuilder.append("${it}\n")
		}
	}
	
	def getTestTags() {
		def listOfSpecFiles = getAllJavascriptSourceFilesInDirectory("${applicationRootDirectory}/web-app/test")
		listOfSpecFiles.collect {
			getSourceFileTag(it)
		}
	}
	
	def getSourceTags() {
		def jsSourceFiles = getAllJavascriptSourceFilesInDirectory("${applicationRootDirectory}/web-app/js")
		jsSourceFiles.collect {
			getSourceFileTag(it)
		}
	}
	
	
	def getAllJavascriptSourceFilesInDirectory(directory) {
		def fileList = getAllFilePathsInDirectory(directory)
		fileList.findAll {it.toUpperCase().endsWith(".JS")}
	}

	def getSourceFileTag(String filePath) {
		def goodString = flipBackSlashesToForwardSlashes(filePath)
		def prefix = '<script type="text/javascript" src="' + FILE_TAG
		def suffix = '"></script>'
		prefix + goodString + suffix
	}

	def getAllFilePathsInDirectory(directory) {
		def fileList = []
		addFilePathsToList(directory, fileList)
		fileList
	}
	
	def addFilePathsToList(directory, fileList) {
		File f1 = new File(directory)
		f1.eachFile() { jsFile ->
			if (!jsFile.name.startsWith(".")) {
				if (jsFile.directory) {
					addFilePathsToList(jsFile.path, fileList)
				} else {
					fileList << jsFile.canonicalPath
				}
			}
		}
	}
	
	def flipBackSlashesToForwardSlashes(String str) {
		str.replaceAll('\\\\', '/')
	}
	
	def getApplicationRootDirectory() {
		flipBackSlashesToForwardSlashes(System.properties['base.dir'])
	}

	def getFilePath() {
		pluginRootDirectory + "web-app/js/SpecRunner.html"
	}
		
	def getPluginRootDirectory() {
		String filePath = getClass().protectionDomain.codeSource.location.path
		def pluginManager = PluginManagerHolder.pluginManager
		def plugin = pluginManager.getGrailsPlugin("jasmine")
		filePath.replace("plugin-classes/", "plugins/jasmine-${plugin.version}/")
	}
	
}
