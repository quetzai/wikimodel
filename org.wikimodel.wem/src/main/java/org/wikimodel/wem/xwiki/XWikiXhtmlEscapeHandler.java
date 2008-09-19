/*******************************************************************************
 * Copyright (c) 2005,2007 Cognium Systems SA and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Contributors:
 *     Cognium Systems SA - initial API and implementation
 *******************************************************************************/
package org.wikimodel.wem.xwiki;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikimodel.wem.xhtml.XhtmlCharacterType;
import org.wikimodel.wem.xhtml.XhtmlCharacter;
import org.wikimodel.wem.xhtml.XhtmlEscapeHandler;

public class XWikiXhtmlEscapeHandler implements XhtmlEscapeHandler
{
    private static final Pattern LIST_PATTERN = Pattern.compile("\\p{Blank}*((\\*+[:;]*)|([1*]+\\.[:;]*)|([:;]+))\\p{Blank}+");
    
    private List<String> fReservedKeywords = Arrays.asList("**", "~~", "##", "--", "__", "^^", ",,", "[[", "]]", "{{", "}}");
    
    public void initialize(Map<String, Object> context)
    {
        context.put("isPotentialList", true);
        context.put("buffer", new StringBuilder());
    }

    public XhtmlCharacter handleCharacter(XhtmlCharacter current, Stack<XhtmlCharacter> characters, String currentTag, Map<String, Object> context)
    {
        XhtmlCharacter result = current;

        StringBuilder buffer = (StringBuilder) context.get("buffer");
        buffer.append(current.getCharacter());
        
        // Escape = symbols when in a header
        if (current.getCharacter() == '=') {
            if (currentTag.equals("h1") || currentTag.equals("h2") || currentTag.equals("h3") 
                || currentTag.equals("h4") || currentTag.equals("h5") || currentTag.equals("h6")) {
                result.setType(XhtmlCharacterType.ESCAPED);
                context.put("isPotentialList", Boolean.FALSE);
                return result;
            }
        }

        // Escape lists
        boolean isPotentialList = (Boolean) context.get("isPotentialList");
        if (isPotentialList && (current.getCharacter() == ' ') && (currentTag.equals("p"))) {
            if (characters.size() > 0) {
                XhtmlCharacter previous = characters.peek();
                if ((previous.getCharacter() == '*') || (previous.getCharacter() == '.') || (previous.getCharacter() == ':') || (previous.getCharacter() == ';')) {
                    // Check if the buffer matches a list format
                    Matcher matcher = LIST_PATTERN.matcher(buffer);
                    if (matcher.matches()) {
                        // Set all chars till the beginning or till the first space as escaped
                        for (int i = characters.size() - 1; i > -1; i--) {
                            XhtmlCharacter ch = characters.elementAt(i);
                            if (ch.getCharacter() == ' ') {
                                break;
                            } else {
                                ch.setType(XhtmlCharacterType.ESCAPED);
                            }
                        }
                    }
                    context.put("isPotentialList", Boolean.FALSE);
                }
            }            
        }
        
        // Escape all reserved keywords
        if (characters.size() > 0) {
            XhtmlCharacter previous = characters.peek();
            if (fReservedKeywords.contains("" + previous.getCharacter() + current.getCharacter())) {
                previous.setType(XhtmlCharacterType.ESCAPED);
                result.setType(XhtmlCharacterType.ESCAPED);
                return result;
            }
        }
        
        return result;
    }
}
