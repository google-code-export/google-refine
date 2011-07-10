/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.importers.parsers;

import java.io.InputStream;

import javax.servlet.ServletException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlParser implements TreeParser{
    final static Logger logger = LoggerFactory.getLogger("XmlParser");
    
    XMLStreamReader parser = null;
    
    public XmlParser(InputStream inputStream){
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_COALESCING, true);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
            parser = factory.createXMLStreamReader(inputStream);
        } catch (XMLStreamException e) {
            // silent
            // e.printStackTrace();
        } catch (FactoryConfigurationError e) {
            // silent
            // e.printStackTrace();
        }
    }
    
    @Override
    public TreeParserToken next() throws ServletException{
        try {
            if(!parser.hasNext())
                throw new ServletException("End of XML stream");
        } catch (XMLStreamException e) {
            throw new ServletException(e);
        }
        
        int currentToken = -1;
        try {
            currentToken = parser.next();
        } catch (XMLStreamException e) {
            throw new ServletException(e);
        }
        
        return mapToTreeParserToken(currentToken);
    }
    
    protected TreeParserToken mapToTreeParserToken(int token) throws ServletException {
        switch(token){
            case XMLStreamConstants.START_ELEMENT: return TreeParserToken.StartEntity;
            case XMLStreamConstants.END_ELEMENT: return TreeParserToken.EndEntity;
            case XMLStreamConstants.CHARACTERS: return TreeParserToken.Value;
            case XMLStreamConstants.START_DOCUMENT: return TreeParserToken.Ignorable;
            case XMLStreamConstants.END_DOCUMENT: return TreeParserToken.Ignorable;
            case XMLStreamConstants.SPACE: return TreeParserToken.Value;
            case XMLStreamConstants.PROCESSING_INSTRUCTION: return TreeParserToken.Ignorable;
            case XMLStreamConstants.NOTATION_DECLARATION: return TreeParserToken.Ignorable;
            case XMLStreamConstants.NAMESPACE: return TreeParserToken.Ignorable;
            case XMLStreamConstants.ENTITY_REFERENCE: return TreeParserToken.Ignorable;
            case XMLStreamConstants.DTD: return TreeParserToken.Ignorable;
            case XMLStreamConstants.COMMENT: return TreeParserToken.Ignorable;
            case XMLStreamConstants.CDATA: return TreeParserToken.Ignorable;
            case XMLStreamConstants.ATTRIBUTE: return TreeParserToken.Ignorable;
            default:
                return TreeParserToken.Ignorable;
        }
    }
    
    @Override
    public TreeParserToken getEventType() throws ServletException{
        return this.mapToTreeParserToken(parser.getEventType());
    }
    
    @Override
    public boolean hasNext() throws ServletException{
        try {
            return parser.hasNext();
        } catch (XMLStreamException e) {
            throw new ServletException(e);
        }
    }
    
    @Override
    public String getLocalName() throws ServletException{
        try{
            return parser.getLocalName();
        }catch(IllegalStateException e){
            return null;
        }
    }
    
    @Override
    public String getPrefix(){
        return parser.getPrefix();
    }
    
    @Override
    public String getText(){
        return parser.getText();
    }
    
    @Override
    public int getAttributeCount(){
        return parser.getAttributeCount();
    }
    
    @Override
    public String getAttributeValue(int index){
        return parser.getAttributeValue(index);
    }
    
    @Override
    public String getAttributePrefix(int index){
        return parser.getAttributePrefix(index);
    }
    
    @Override
    public String getAttributeLocalName(int index){
        return parser.getAttributeLocalName(index);
    }
}
