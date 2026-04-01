import os
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Find the class name
    class_match = re.search(r'class\s+([A-Za-z0-9_]+)', content)
    if not class_match:
        # Check for object
        class_match = re.search(r'object\s+([A-Za-z0-9_]+)', content)
        if not class_match:
            return

    class_name = class_match.group(1)
    
    # We want to insert log.debug("[ClassName] --MethodName") at the start of functions
    # function signature: fun methodName(args...) {
    
    # A regex to match fun declarations that have a block body '{'
    # We need to capture the function name.
    # We'll skip inline/single-expression functions for simplicity if they just use '=' instead of '{'
    
    # Let's use a simple replacement
    lines = content.split('\n')
    new_lines = []
    
    changed = False
    needs_import = False
    inside_class = False

    for i, line in enumerate(lines):
        new_lines.append(line)
        
        # very basic check for import
        if line.startswith("import ") and "com.llr.rideapp.utils.log" not in content:
            needs_import = True
            
        fun_match = re.match(r'^(\s*)((?:override\s+|private\s+|public\s+|suspend\s+|protected\s+)*)fun\s+(?:<[^>]+>\s+)?([a-zA-Z0-9_]+)\s*\(.*?\)(?:\s*:\s*[a-zA-Z0-9_<>\?]+)?\s*\{', line)
        if fun_match:
            indent = fun_match.group(1)
            fun_name = fun_match.group(3)
            log_stmt = f'{indent}    com.llr.rideapp.utils.log.debug("[{class_name}] --{fun_name}")'
            new_lines.append(log_stmt)
            changed = True
            
        # Match init block
        init_match = re.match(r'^(\s*)init\s*\{', line)
        if init_match:
            indent = init_match.group(1)
            log_stmt = f'{indent}    com.llr.rideapp.utils.log.debug("[{class_name}] --init")'
            new_lines.append(log_stmt)
            changed = True

    if changed:
        final_content = "\n".join(new_lines)
        if needs_import and "import com.llr.rideapp.utils.log" not in final_content:
            # add import after the package declaration
            final_content = re.sub(r'(package com\.llr\.rideapp\.[a-zA-Z0-9_.]+)', r'\1\n\nimport com.llr.rideapp.utils.log', final_content, count=1)
        
        with open(filepath, 'w') as f:
            f.write(final_content)
        print(f"Added logs to {filepath}")

directories = [
    "app/src/main/java/com/llr/rideapp/presentation",
    "app/src/main/java/com/llr/rideapp/data/repository",
    "app/src/main/java/com/llr/rideapp/data/local",
    "app/src/main/java/com/llr/rideapp/data/remote/interceptor"
]

for d in directories:
    for root, _, files in os.walk(d):
        for file in files:
            if file.endswith(".kt") and "Screen" not in file and "State" not in file:
                process_file(os.path.join(root, file))

