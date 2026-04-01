import re

file_path = "/home/jordan-tsagmo/Desktop/PROJET/2025-2026/FREELANCE/LLR CONSULTING/CODE/app/src/main/java/com/llr/rideapp/presentation/client/ClientDashboardScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# The popup code starts at "if (orderPopupVehicule != null) {" and ends at the end of the file
# Let's extract it manually
start_idx = content.find("        if (orderPopupVehicule != null) {")
if start_idx != -1:
    popup_code = content[start_idx:]
    # Remove from original
    content = content[:start_idx]
    
    # Clean up trailing braces in content
    content = content.rstrip()
    if not content.endswith("}"):
        content += "\n    }\n}\n"
    elif content.endswith("    }"):
         content += "\n}\n"
    
    # We must insert popup_code inside ClientDashboardScreen
    # It ends at:
    #                 onCommandVehicule = { orderPopupVehicule = it }
    #             )
    #         }
    #     }
    # }
    
    insert_point = content.find("onCommandVehicule = { orderPopupVehicule = it }\n                )\n            }\n        }\n    }")
    if insert_point != -1:
        # We replace the end block
        target = "onCommandVehicule = { orderPopupVehicule = it }\n                )\n            }\n        }\n"
        
        # Format popup_code, removing extra trailing braces that might have been imported
        # We know popup_code has extra braces at the end.
        popup_code = popup_code.rstrip()
        while popup_code.endswith("}"):
            popup_code = popup_code[:-1].rstrip()
            
        full_replacement = target + "        " + popup_code + "\n    }\n}\n"
        
        content = content.replace(target + "    }", full_replacement)

with open(file_path, "w") as f:
    f.write(content)
print("Done fix2.")

