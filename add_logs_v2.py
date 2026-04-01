import os
import re

LOG_IMPORT = "import com.llr.rideapp.utils.log"

def process_file(filepath):
    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except:
        return

    # Keep track if we change it
    original_content = content
    
    # Extract class or object name
    # Can match multiple classes, but let's assume one main class per file based on file name or first class
    file_name = os.path.basename(filepath).replace(".kt", "")
    class_name = file_name
    
    # A robust way to insert log at the beginning of function bodies.
    # We will search for fun name(...) {
    
    lines = content.split('\n')
    new_lines = []
    changed = False

    # Check if importing log is necessary
    has_log_import = LOG_IMPORT in content

    for line in lines:
        new_lines.append(line)
        
        # heuristic to find functions:
        # starts with (optional modifiers) fun (optional generic) methodName(
        # ends with {
        
        fun_match = re.search(r'^(\s*)(?:(?:override|private|public|protected|internal|suspend|inline)\s+)*fun\s+(?:<[^>]+>\s+)?([a-zA-Z0-9_]+)\s*\(.*?\)(?:\s*:\s*[a-zA-Z0-9_<>,\?\s]+)?\s*\{\s*$', line)
        
        if fun_match:
            indent = fun_match.group(1)
            method_name = fun_match.group(2)
            log_stmt = f'{indent}    log.debug("[{class_name}] --{method_name}")'
            # Prevent double logging
            if log_stmt not in original_content:
                new_lines.append(log_stmt)
                changed = True
                
        init_match = re.search(r'^(\s*)init\s*\{\s*$', line)
        if init_match:
            indent = init_match.group(1)
            log_stmt = f'{indent}    log.debug("[{class_name}] --init")'
            if log_stmt not in original_content:
                new_lines.append(log_stmt)
                changed = True

    if changed:
        final_content = "\n".join(new_lines)
        if not has_log_import and "log.debug" in final_content:
            final_content = re.sub(r'^(package .*)$', r'\1\n\n' + LOG_IMPORT, final_content, count=1, flags=re.MULTILINE)
        
        with open(filepath, 'w') as f:
            f.write(final_content)
        print(f"Updated {filepath}")

search_dirs = [
    "app/src/main/java/com/llr/rideapp/presentation",
    "app/src/main/java/com/llr/rideapp/data",
    "app/src/main/java/com/llr/rideapp/domain"
]

for d in search_dirs:
    for root, _, files in os.walk(d):
        for file in files:
            if file.endswith(".kt") and file != "LogUtils.kt":
                process_file(os.path.join(root, file))

