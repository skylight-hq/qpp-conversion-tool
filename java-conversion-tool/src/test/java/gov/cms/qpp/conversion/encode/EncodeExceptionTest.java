package gov.cms.qpp.conversion.encode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
/**
 * Test to provide Circle CI Coverage on EncodeException
 */
public class EncodeExceptionTest {
	@Test
	public void getTemplateId() throws Exception {
		EncodeException e = new EncodeException("ErrorMessage");
		e.setTemplateId("templateId");
		String value = e.getTemplateId();
		assertThat("Expect to get out what I put in", value, is("templateId"));
	}

}