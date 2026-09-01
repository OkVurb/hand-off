import re
with open('src/main/java/com/studio/planeshift/common/course/CourseState.java', 'r') as f:
    text = f.read()

text = text.replace('boolean autoScroll\n) {', 'boolean autoScroll,\n        CourseTheme theme\n) {')
text = text.replace('DEFAULT_KILL_Y, 0, NO_TIME_LIMIT, false);', 'DEFAULT_KILL_Y, 0, NO_TIME_LIMIT, false, CourseTheme.GRASS);')

text = text.replace('.forGetter(CourseState::autoScroll)\n    ).apply(', '.forGetter(CourseState::autoScroll),\n            CourseTheme.CODEC.optionalFieldOf(\"theme\", CourseTheme.GRASS).forGetter(CourseState::theme)\n    ).apply(')
text = text.replace('lives, killY, score, timeLeft, autoScroll) ->', 'lives, killY, score, timeLeft, autoScroll, theme) ->')
text = text.replace('score, timeLeft, autoScroll))));', 'score, timeLeft, autoScroll, theme))));')

text = text.replace('boolean autoScroll = ByteBufCodecs.BOOL.decode(buf);', 'boolean autoScroll = ByteBufCodecs.BOOL.decode(buf);\n            CourseTheme theme = ByteBufCodecs.fromCodec(CourseTheme.CODEC).decode(buf);')
text = text.replace('score, timeLeft, autoScroll);', 'score, timeLeft, autoScroll, theme);')
text = text.replace('ByteBufCodecs.BOOL.encode(buf, state.autoScroll());', 'ByteBufCodecs.BOOL.encode(buf, state.autoScroll());\n            ByteBufCodecs.fromCodec(CourseTheme.CODEC).encode(buf, state.theme());')

text = re.sub(r'new CourseState\((.*?),\s*autoScroll\)', r'new CourseState(\1, autoScroll, theme)', text, flags=re.DOTALL)

with open('src/main/java/com/studio/planeshift/common/course/CourseState.java', 'w') as f:
    f.write(text)
