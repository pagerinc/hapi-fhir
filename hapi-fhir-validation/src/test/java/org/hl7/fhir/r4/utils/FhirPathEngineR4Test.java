package org.hl7.fhir.r4.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.TestUtil;
import org.hl7.fhir.dstu3.utils.FhirPathEngineTest;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class FhirPathEngineR4Test {

	private static FhirContext ourCtx = FhirContext.forR4();
	private static FHIRPathEngine ourEngine;
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirPathEngineTest.class);

	@Test
	public void testCrossResourceBoundaries() throws FHIRException {
		Specimen specimen = new Specimen();
		specimen.setId("#FOO");
		specimen.setReceivedTimeElement(new DateTimeType("2011-01-01"));
		Observation o = new Observation();
		o.getContained().add(specimen);

		o.setId("O1");
		o.setStatus(Observation.ObservationStatus.FINAL);
		o.setSpecimen(new Reference("#FOO"));

		List<Base> value = ourEngine.evaluate(o, "Observation.specimen.resolve().receivedTime");
		assertEquals(1, value.size());
		assertEquals("2011-01-01", ((DateTimeType) value.get(0)).getValueAsString());
	}

	@Test
	public void testAs() throws Exception {
		Observation obs = new Observation();
		obs.setValue(new StringType("FOO"));
		
		List<Base> value = ourEngine.evaluate(obs, "Observation.value.as(String)");
		assertEquals(1, value.size());
		assertEquals("FOO", ((StringType)value.get(0)).getValue());
	}
	
	@Test
	public void testExistsWithNoValue() throws FHIRException {
		Patient patient = new Patient();
		patient.setDeceased(new BooleanType());
		List<Base> eval = ourEngine.evaluate(patient, "Patient.deceased.exists()");
		ourLog.info(eval.toString());
		assertFalse(((BooleanType)eval.get(0)).getValue());
	}

	@Test
	public void testApproxEquivalent() throws FHIRException {
		Patient patient = new Patient();
		patient.setDeceased(new BooleanType());
		testEquivalent(patient, "@2012-04-15 ~ @2012-04-15",true);
		testEquivalent(patient, "@2012-04-15 ~ @2012-04-15T10:00:00",true);
	}

	@Test
	public void testApproxNotEquivalent() throws FHIRException {
		Patient patient = new Patient();
		patient.setDeceased(new BooleanType());
		testEquivalent(patient, "@2012-04-15 !~ @2012-04-15",false);
		testEquivalent(patient, "@2012-04-15 !~ @2012-04-15T10:00:00",false);
	}


	private void testEquivalent(Patient thePatient, String theExpression, boolean theExpected) throws FHIRException {
		List<Base> eval = ourEngine.evaluate(thePatient, theExpression);
		assertEquals(theExpected, ((BooleanType)eval.get(0)).getValue());
	}

	@Test
	public void testExistsWithValue() throws FHIRException {
		Patient patient = new Patient();
		patient.setDeceased(new BooleanType(false));
		List<Base> eval = ourEngine.evaluate(patient, "Patient.deceased.exists()");
		ourLog.info(eval.toString());
		assertTrue(((BooleanType)eval.get(0)).getValue());
	}

	@Test
	public void testConcatenation() throws FHIRException {
		String exp = "Patient.name.family & '.'";

		Patient p = new Patient();
		p.addName().setFamily("TEST");
		String result = ourEngine.evaluateToString(p, exp);
		assertEquals("TEST.", result);
	}

	@Test
	public void testConcatenationFunction() throws FHIRException {
		String exp = "element.first().path.startsWith(%resource.type) and element.tail().all(path.startsWith(%resource.type&'.'))";

		StructureDefinition sd = new StructureDefinition();
		StructureDefinition.StructureDefinitionDifferentialComponent diff = sd.getDifferential();

		diff.addElement().setPath("Patient.name");


		Patient p = new Patient();
		p.addName().setFamily("TEST");
		List<Base> result = ourEngine.evaluate(null, p, diff, exp);
		ourLog.info(result.toString());
//		assertEquals("TEST.", result);
	}


	@AfterClass
	public static void afterClassClearContext() throws Exception {
		TestUtil.clearAllStaticFieldsForUnitTest();
	}

	@BeforeClass
	public static void beforeClass() {
		ourEngine = new FHIRPathEngine(new HapiWorkerContext(ourCtx, new DefaultProfileValidationSupport()));
	}

}
