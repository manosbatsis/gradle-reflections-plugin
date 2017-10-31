package foo.bar.baz;

import java.io.Serializable;

/**
 * Just a sample class
 */
public class Example implements Serializable {

	public String stringMember;

	public String getStringMember() {
		return stringMember;
	}

	public void setStringMember(String stringMember) {
		this.stringMember = stringMember;
	}
}