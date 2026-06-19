import sys

def check_braces(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    stack = []
    lazy_col_opened_at = -1
    
    for i, line in enumerate(lines):
        line_num = i + 1
        
        # Check for LazyColumn opening
        if 'LazyColumn' in line:
            lazy_col_opened = True
            
        for char in line:
            if char == '{':
                stack.append(line_num)
                if 'LazyColumn' in line and lazy_col_opened_at == -1:
                    lazy_col_opened_at = len(stack)
            elif char == '}':
                if stack:
                    popped = stack.pop()
                    if len(stack) < lazy_col_opened_at:
                        print(f"LazyColumn (opened at depth {lazy_col_opened_at}) was closed! at line {line_num} which matched {popped}")
                        lazy_col_opened_at = -1
                else:
                    print(f"Unmatched closing brace at line {line_num}")

    print(f"Remaining open braces: {len(stack)}")
    if stack:
        print(f"Last unclosed brace opened at: {stack[-1]}")

check_braces('app/src/main/java/com/example/ui/IntroDashboardScreen.kt')
