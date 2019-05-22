/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.core;

import com.intuit.karate.StringUtils;

import java.util.Objects;

/**
 * someday this will be part of the parser, but until then apologies for this
 * monstrosity :|
 *
 * @author pthomas3
 */
public class MatchStep {

    public final String name;
    public final String path;
    public final MatchType type;
    public final String expected;

    public MatchStep(String raw) {
        boolean each = false;
        raw = raw.trim();
        if (raw.startsWith("each")) {
            each = true;
            raw = raw.substring(4).trim();
        }
        boolean contains = false;
        boolean not;
        boolean only = false;
        boolean any = false;
        int spacePos = raw.indexOf(' ');
        int leftParenPos = raw.indexOf('(');
        int rightParenPos = raw.indexOf(')');
        int lhsEndPos = raw.indexOf(" contains");
        if (lhsEndPos == -1) {
            lhsEndPos = raw.indexOf(" !contains");
        }
        int searchPos = 0;
        int eqPos = raw.indexOf(" == ");
        if (eqPos == -1) {
            eqPos = raw.indexOf(" != ");
        }
        if (lhsEndPos != -1 && (eqPos == -1 || eqPos > lhsEndPos)) {
            contains = true;
            not = raw.charAt(lhsEndPos + 1) == '!';
            searchPos = lhsEndPos + (not ? 10 : 9);
            String anyOrOnly = raw.substring(searchPos).trim();
            if (anyOrOnly.startsWith("only")) {
                int onlyPos = raw.indexOf(" only", searchPos);
                only = true;
                searchPos = onlyPos + 5;
            } else if (anyOrOnly.startsWith("any")) {
                int anyPos = raw.indexOf(" any", searchPos);
                any = true;
                searchPos = anyPos + 4;
            }
        } else {
            int equalPos = raw.indexOf(" ==", searchPos);
            int notEqualPos = raw.indexOf(" !=", searchPos);
            if (equalPos == -1 && notEqualPos == -1) {
                throw new RuntimeException("syntax error, expected '==' for match");
            }
            lhsEndPos = min(equalPos, notEqualPos);
            if (lhsEndPos > spacePos && rightParenPos != -1 && rightParenPos > lhsEndPos) {
                equalPos = raw.indexOf(" ==", rightParenPos);
                notEqualPos = raw.indexOf(" !=", rightParenPos);
                if (equalPos == -1 && notEqualPos == -1) {
                    throw new RuntimeException("syntax error, expected '==' for match");
                }
                lhsEndPos = min(equalPos, notEqualPos);
            }
            not = lhsEndPos == notEqualPos;
            searchPos = lhsEndPos + 3;
        }
        String lhs = raw.substring(0, lhsEndPos).trim();
        if (leftParenPos == -1) {
            leftParenPos = lhs.indexOf('[');
        }
        if (spacePos != -1 && (leftParenPos > spacePos || leftParenPos == -1)) {
            name = lhs.substring(0, spacePos);
            path = StringUtils.trimToNull(lhs.substring(spacePos));
        } else {
            name = lhs;
            path = null;
        }
        expected = StringUtils.trimToNull(raw.substring(searchPos));
        type = getType(each, not, contains, only, any);
    }

    MatchStep(String name, String path, MatchType type, String expected) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.expected = expected;
    }

    private static int min(int a, int b) {
        if (a == -1) {
            return b;
        }
        if (b == -1) {
            return a;
        }
        return Math.min(a, b);
    }

    private static MatchType getType(boolean each, boolean not, boolean contains, boolean only, boolean any) {
        if (each) {
            if (contains) {
                if (only) {
                    return MatchType.EACH_CONTAINS_ONLY;
                }
                if (any) {
                    return MatchType.EACH_CONTAINS_ANY;
                }
                return not ? MatchType.EACH_NOT_CONTAINS : MatchType.EACH_CONTAINS;
            }
            return not ? MatchType.EACH_NOT_EQUALS : MatchType.EACH_EQUALS;
        }
        if (contains) {
            if (only) {
                return MatchType.CONTAINS_ONLY;
            }
            if (any) {
                return MatchType.CONTAINS_ANY;
            }
            return not ? MatchType.NOT_CONTAINS : MatchType.CONTAINS;
        }
        return not ? MatchType.NOT_EQUALS : MatchType.EQUALS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchStep matchStep = (MatchStep) o;
        return name.equals(matchStep.name) &&
                Objects.equals(path, matchStep.path) &&
                type == matchStep.type &&
                Objects.equals(expected, matchStep.expected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, type, expected);
    }
}
