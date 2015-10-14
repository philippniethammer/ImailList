package imailList;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;

import org.junit.BeforeClass;
import org.junit.Test;

public class ListMessageHandlerTest {
	
	private static String TAG = "[Test]";
	
	@BeforeClass
	public static void setupClass() {
	}
	
	private String removePrefixQueues(String subject) {
		String result;
		Pattern pRE = Pattern.compile(
				"((?i:RE|AW)^?[0-9]*:)\\s*("+ Pattern.quote(TAG) +")?"
				+ "\\s*(?:(?i:RE|AW)^?[0-9]*:\\s*(?:"+ Pattern.quote(TAG) + ")?\\s*)*");
		Pattern pFWD = Pattern.compile(
				"((?i:FWD|WG)^?[0-9]*:)\\s*("+ Pattern.quote(TAG) +")?"
				+ "\\s*(?:(?i:FWD|WG)^?[0-9]*:\\s*(?:"+ Pattern.quote(TAG) + ")?\\s*)*");
		
		result =  pRE.matcher(subject).replaceAll("Re: $2 ");
		result =  pFWD.matcher(result).replaceAll("Fwd: $2 ");
		System.out.println(result);
		
		Pattern p = Pattern.compile(
				"^((?:(?i:RE|AW|FWD|WG)^?[0-9]*:\\s*)*(?i:RE|AW|FWD|WG)^?[0-9]*:)\\s*("+ Pattern.quote(TAG) +")"
				+ "\\s*((?i:RE|AW|FWD|WG)^?[0-9]*:)\\s*(?:"+ Pattern.quote(TAG) + "\\s*)?");
		Matcher m = p.matcher(result);
		while (m.find()) {
			result = m.replaceFirst("$1 $3 $2 ");
			m = p.matcher(result);
		}
		
		return result;
	}

	@Test
	public void testSubjectRewriting() {
		assertEquals("Re: [Test] ", removePrefixQueues("AW: [Test] Aw: [Test]"));
		assertEquals("Fwd: [Test] ", removePrefixQueues("WG: [Test]"));
		assertEquals("Fwd: Re: [Test] ", removePrefixQueues("WG: [Test] RE: AW:"));
		assertEquals("Re: Fwd: Re: [Test] ", removePrefixQueues("Re: [Test] WG: [Test] Re: [Test]"));
	}

}
