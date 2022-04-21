package edu.upenn.cis.cis455.xpathengine;

interface XPathEngine {
    /**
     * Sets the XPath expression(s) that are to be evaluated.
     * 
     * @param expressions
     */
    void setXPaths(String[] expressions);

    /**
     * Event driven pattern match.
     * 
     * Takes an event at a time as input
     *
     * @param event notification about something parsed, from a given document
     * 
     * @return bit vector of matches to XPaths
     */
    boolean[] evaluateEvent(OccurrenceEvent event);

}
