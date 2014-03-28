package cn.ingenic.glasssync.vcalendar;

import java.util.List;

public interface VCalendarInterpreter {

	/**
	 * Called when vCalendar interpretation started
	 */
	void start();
	/**
	 * Called when vCalendar interpretation finished.
	 */
	void end();
	/**
	 * 
	 */
	void startEntry();
	/**
	 * 
	 */
	void endEntry();
	/**
	 * 
	 */
	void startProperty();
	/**
	 * 
	 */
	void endProperty();
	/**
	 * 
	 */
	void propertyGroup(String group);
	/**
	 * @param name A property name like "DESCRIPTION"
	 */
	void propertyName(String name);
	/**
     * @param type A parameter name like "ENCODING", "CHARSET", etc.
     */
    void propertyParamType(String type);

    /**
     * @param value A parameter value. This method may be called without
     * {@link #propertyParamType(String)} being called (when the vCard is vCard 2.1).
     */
    void propertyParamValue(String value);

    /**
     * @param values List of property values. The size of values would be 1 unless
     * coressponding property name is "N", "ADR", or "ORG".
     */
    void propertyValue(String values);
	
}
