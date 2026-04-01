import re

file_path = "/home/jordan-tsagmo/Desktop/PROJET/2025-2026/FREELANCE/LLR CONSULTING/CODE/app/src/main/java/com/llr/rideapp/presentation/client/ClientDashboardScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# Extract the popup code from VehiculeCard
popup_code_match = re.search(r'(        if \(orderPopupVehicule \!= null\) \{.*\n  \})', content, flags=re.DOTALL)
if popup_code_match:
    popup_code = popup_code_match.group(1)
    content = content.replace(popup_code, "") # remove from current location

    # Find the end of ClientDashboardScreen content
    # ClientDashboardScreen ends after VehiculeListSection
    insert_target = r"""                VehiculeListSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    vehicules = vehicules,
                    selectedClass = selectedClass,
                    onClassSelected = { viewModel.selectClass(it) },
                    onVehiculeTap = { selectedVehicule = it },
                    onCommandVehicule = { orderPopupVehicule = it }
                )
            }
        }
    }"""
    
    # fix the indentation
    popup_code_indented = "\n".join(["    " + line for line in popup_code.split("\n")])

    content = content.replace(insert_target, insert_target + "\n\n" + popup_code_indented)

with open(file_path, "w") as f:
    f.write(content)

history_path = "/home/jordan-tsagmo/Desktop/PROJET/2025-2026/FREELANCE/LLR CONSULTING/CODE/app/src/main/java/com/llr/rideapp/presentation/client/ClientRideHistoryScreen.kt"
try:
    with open(history_path, "r") as f:
        h_content = f.read()
    h_content = h_content.replace('ride.createdAt', 'ride.requestedAt ?: ""')
    with open(history_path, "w") as f:
        f.write(h_content)
except Exception as e:
    print(e)
print("Fixes applied.")
