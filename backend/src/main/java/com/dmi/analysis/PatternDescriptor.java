package com.dmi.analysis;

import java.util.regex.Pattern;

public record PatternDescriptor(int index, String patternKey, Pattern compiled) {}
