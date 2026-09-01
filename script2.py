import re

with open('src/main/java/com/studio/planeshift/common/course/CourseState.java', 'r') as f:
    text = f.read()

# Add theme field to record definition
text = text.replace('boolean autoScroll\n) {', 'boolean autoScroll,\n        CourseTheme theme\n) {')

# Add theme to DEFAULT
text = text.replace('DEFAULT_KILL_Y, 0, NO_TIME_LIMIT, false);', 'DEFAULT_KILL_Y, 0, NO_TIME_LIMIT, false, CourseTheme.GRASS);')

# Add theme to MapCodec
text = text.replace('.forGetter(CourseState::autoScroll)\n    ).apply(', '.forGetter(CourseState::autoScroll),\n            CourseTheme.CODEC.optionalFieldOf(\"theme\", CourseTheme.GRASS).forGetter(CourseState::theme)\n    ).apply(')
text = text.replace('lives, killY, score, timeLeft, autoScroll) ->', 'lives, killY, score, timeLeft, autoScroll, theme) ->')
text = text.replace('score, timeLeft, autoScroll))));', 'score, timeLeft, autoScroll, theme))));')

# Add theme to StreamCodec
text = text.replace('boolean autoScroll = ByteBufCodecs.BOOL.decode(buf);', 'boolean autoScroll = ByteBufCodecs.BOOL.decode(buf);\n            CourseTheme theme = CourseTheme.CODEC.decode(buf);')
text = text.replace('score, timeLeft, autoScroll);', 'score, timeLeft, autoScroll, theme);')
text = text.replace('ByteBufCodecs.BOOL.encode(buf, state.autoScroll());', 'ByteBufCodecs.BOOL.encode(buf, state.autoScroll());\n            CourseTheme.CODEC.encode(buf, state.theme());')

# Add theme to all withX methods
text = re.sub(r'new CourseState\((.*?),\s*autoScroll\)', r'new CourseState(\1, autoScroll, theme)', text, flags=re.DOTALL)
# But wait! StreamCodec decode has a new CourseState which uses 	heme as a local variable. So the regex will make it utoScroll, theme). That's correct!
# The CODEC apply has a 
ew CourseState which uses 	heme as a local variable. The regex will make it utoScroll, theme). That's correct!
# Wait, in the RecordCodecBuilder.mapCodec... it says sanitize(new CourseState(... 

with open('src/main/java/com/studio/planeshift/common/course/CourseState.java', 'w') as f:
    f.write(text)
