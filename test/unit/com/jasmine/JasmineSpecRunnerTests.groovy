package com.jasmine

import org.junit.Test
import com.jasmine.JasmineSpecRunner

class JasmineSpecRunnerTests {

	@Test
	void getNumberOfFailuresWithNoFailures() {
		def specRunner = new JasmineSpecRunner()
		def slurper = new XmlSlurper()
		def htmlNodes = slurper.parseText('<a class="description" href="?">6 specs, 0 failures in 0.062s</a>')
		
		def result = specRunner.getNumberOfFailures(htmlNodes)
		
		assert 0 == result
	}
	
	@Test
	void getNumberOfFailuresWithSingleFailure() {
		def specRunner = new JasmineSpecRunner()
		def slurper = new XmlSlurper()
		def htmlNodes = slurper.parseText('<a class="description" href="?">6 specs, 1 failure in 0.062s</a>')
		
		def result = specRunner.getNumberOfFailures(htmlNodes)
		
		assert 1 == result
	}
	
	@Test
	void getNumberOfFailuresWithMultipleFailures() {
		def specRunner = new JasmineSpecRunner()
		def slurper = new XmlSlurper()
		def htmlNodes = slurper.parseText('<a class="description" href="?">6 specs, 2 failures in 0.062s</a>')
		
		def result = specRunner.getNumberOfFailures(htmlNodes)
		
		assert 2 == result
	}
	
}
