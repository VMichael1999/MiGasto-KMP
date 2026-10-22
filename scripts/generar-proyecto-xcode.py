#!/usr/bin/env python3
"""Genera iosApp/MiGasto.xcodeproj a partir de lo que hay en iosApp/MiGasto.

Se usa en vez de XcodeGen para no sumar una herramienta más. Después de correrlo hay que volver a hacer
`pod install` dentro de iosApp (el proyecto generado no trae la integración de CocoaPods):

    python3 scripts/generar-proyecto-xcode.py && (cd iosApp && pod install)
"""
import hashlib
import os

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "iosApp")
NOMBRE = "MiGasto"
BUNDLE = "com.migasto.ios"
# La app es para iOS 26: la barra de pestañas con Liquid Glass y la API Tab son de esa versión. Las pods siguen en 17.
DESPLIEGUE = "26.0"



def uid(texto):
    return hashlib.md5(texto.encode()).hexdigest()[:24].upper()


def archivos(carpeta, extensiones):
    salida = []
    base = os.path.join(RAIZ, carpeta)
    for dirpath, _, nombres in os.walk(base):
        for n in sorted(nombres):
            if n.endswith(extensiones):
                salida.append(os.path.relpath(os.path.join(dirpath, n), RAIZ))
    return sorted(salida)


# La extensión «Compartir»: lee con Vision las capturas que el usuario comparte y las deja en una cola de la app.
EXT = "MiGastoCompartir"
BUNDLE_EXT = BUNDLE + ".compartir"
fuentes = archivos(NOMBRE, (".swift",))
fuentes_ext = archivos(EXT, (".swift",))
# Estos archivos están en la app y también se compilan dentro de la extensión.
compartidas = [os.path.join(NOMBRE, "Compartido", "ColaDeCapturas.swift")]
recursos = archivos(os.path.join(NOMBRE, "Resources"), (".ttf", ".otf"))

objetos = []  # (id, comentario, cuerpo)


def obj(id_, comentario, cuerpo):
    objetos.append((id_, comentario, cuerpo))


def q(s):
    return '"' + s.replace('"', '\\"') + '"'


# Referencias a archivos
refs = {}
for ruta in fuentes:
    refs[ruta] = uid("ref" + ruta)
    obj(refs[ruta], os.path.basename(ruta), f"isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {q(os.path.basename(ruta))}; sourceTree = \"<group>\";")
for ruta in recursos:
    refs[ruta] = uid("ref" + ruta)
    obj(refs[ruta], os.path.basename(ruta), f"isa = PBXFileReference; lastKnownFileType = file; path = {q(os.path.basename(ruta))}; sourceTree = \"<group>\";")
ref_info = uid("ref-info")
obj(ref_info, "Info.plist", "isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = \"<group>\";")
ref_assets = uid("ref-assets")
obj(ref_assets, "Assets.xcassets", "isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = Assets.xcassets; sourceTree = \"<group>\";")
ref_ent_app = uid("ref-ent-app")
obj(ref_ent_app, f"{NOMBRE}.entitlements", f"isa = PBXFileReference; lastKnownFileType = text.plist.entitlements; path = {NOMBRE}.entitlements; sourceTree = \"<group>\";")
ref_ent_ext = uid("ref-ent-ext")
obj(ref_ent_ext, f"{EXT}.entitlements", f"isa = PBXFileReference; lastKnownFileType = text.plist.entitlements; path = {EXT}.entitlements; sourceTree = \"<group>\";")
ref_info_ext = uid("ref-info-ext")
obj(ref_info_ext, "Info.plist", "isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = \"<group>\";")
for ruta in fuentes_ext:
    refs[ruta] = uid("ref" + ruta)
    obj(refs[ruta], os.path.basename(ruta), f"isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {q(os.path.basename(ruta))}; sourceTree = \"<group>\";")
ref_producto_ext = uid("ref-producto-ext")
obj(ref_producto_ext, f"{EXT}.appex", f"isa = PBXFileReference; explicitFileType = \"wrapper.app-extension\"; includeInIndex = 0; path = {EXT}.appex; sourceTree = BUILT_PRODUCTS_DIR;")
ref_producto = uid("ref-producto")
obj(ref_producto, f"{NOMBRE}.app", f"isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = {NOMBRE}.app; sourceTree = BUILT_PRODUCTS_DIR;")

# Archivos de compilación
build = {}
for ruta in fuentes + recursos:
    build[ruta] = uid("build" + ruta)
    obj(build[ruta], f"{os.path.basename(ruta)} in " + ("Sources" if ruta in fuentes else "Resources"), f"isa = PBXBuildFile; fileRef = {refs[ruta]};")
build_ext = {}
for ruta in fuentes_ext + compartidas:
    build_ext[ruta] = uid("buildext" + ruta)
    obj(build_ext[ruta], f"{os.path.basename(ruta)} in Sources", f"isa = PBXBuildFile; fileRef = {refs[ruta]};")
build_embed = uid("build-embed-ext")
obj(build_embed, f"{EXT}.appex in Embed Foundation Extensions", f"isa = PBXBuildFile; fileRef = {ref_producto_ext}; settings = {{ATTRIBUTES = (RemoveHeadersOnCopy, ); }};")
build_assets = uid("build-assets")
obj(build_assets, "Assets.xcassets in Resources", f"isa = PBXBuildFile; fileRef = {ref_assets};")

# Grupos
def grupo_de(prefijo, nombre):
    hijos = [r for r in (fuentes + recursos) if os.path.dirname(r) == prefijo]
    return hijos

g_resources = uid("grupo-resources")
obj(g_resources, "Resources", "isa = PBXGroup; children = (\n" + "".join(f"\t\t\t\t{refs[r]},\n" for r in recursos) + "\t\t\t); path = Resources; sourceTree = \"<group>\";")
g_app = uid("grupo-app")
raiz_swift = [r for r in fuentes if os.path.dirname(r) == NOMBRE]
subcarpetas = sorted({os.path.dirname(r) for r in fuentes if os.path.dirname(r) != NOMBRE})
g_sub = {}
for sc in subcarpetas:
    g_sub[sc] = uid("grupo" + sc)
    hijos = [r for r in fuentes if os.path.dirname(r) == sc]
    obj(g_sub[sc], os.path.basename(sc), "isa = PBXGroup; children = (\n" + "".join(f"\t\t\t\t{refs[r]},\n" for r in hijos) + f"\t\t\t); path = {q(os.path.basename(sc))}; sourceTree = \"<group>\";")
hijos_app = [refs[r] for r in raiz_swift] + list(g_sub.values()) + [g_resources, ref_assets, ref_info, ref_ent_app]
obj(g_app, NOMBRE, "isa = PBXGroup; children = (\n" + "".join(f"\t\t\t\t{h},\n" for h in hijos_app) + f"\t\t\t); path = {NOMBRE}; sourceTree = \"<group>\";")
g_ext = uid("grupo-ext")
hijos_ext = [refs[r] for r in fuentes_ext] + [ref_info_ext, ref_ent_ext]
obj(g_ext, EXT, "isa = PBXGroup; children = (\n" + "".join(f"\t\t\t\t{h},\n" for h in hijos_ext) + f"\t\t\t); path = {EXT}; sourceTree = \"<group>\";")
g_productos = uid("grupo-productos")
obj(g_productos, "Products", f"isa = PBXGroup; children = (\n\t\t\t\t{ref_producto},\n\t\t\t\t{ref_producto_ext},\n\t\t\t); name = Products; sourceTree = \"<group>\";")
g_raiz = uid("grupo-raiz")
obj(g_raiz, "", f"isa = PBXGroup; children = (\n\t\t\t\t{g_app},\n\t\t\t\t{g_ext},\n\t\t\t\t{g_productos},\n\t\t\t); sourceTree = \"<group>\";")

# Fases
f_fuentes, f_frameworks, f_recursos = uid("fase-fuentes"), uid("fase-frameworks"), uid("fase-recursos")
obj(f_fuentes, "Sources", "isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = (\n" + "".join(f"\t\t\t\t{build[r]},\n" for r in fuentes) + "\t\t\t); runOnlyForDeploymentPostprocessing = 0;")
obj(f_frameworks, "Frameworks", "isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (\n\t\t\t); runOnlyForDeploymentPostprocessing = 0;")
obj(f_recursos, "Resources", "isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = (\n" + "".join(f"\t\t\t\t{build[r]},\n" for r in recursos) + f"\t\t\t\t{build_assets},\n\t\t\t); runOnlyForDeploymentPostprocessing = 0;")

f_fuentes_ext, f_frameworks_ext, f_embed = uid("fase-fuentes-ext"), uid("fase-frameworks-ext"), uid("fase-embed-ext")
obj(f_fuentes_ext, "Sources", "isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = (\n" + "".join(f"\t\t\t\t{build_ext[r]},\n" for r in fuentes_ext + compartidas) + "\t\t\t); runOnlyForDeploymentPostprocessing = 0;")
obj(f_frameworks_ext, "Frameworks", "isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (\n\t\t\t); runOnlyForDeploymentPostprocessing = 0;")
obj(f_embed, "Embed Foundation Extensions", f"isa = PBXCopyFilesBuildPhase; buildActionMask = 2147483647; dstPath = \"\"; dstSubfolderSpec = 13; files = (\n\t\t\t\t{build_embed},\n\t\t\t); name = \"Embed Foundation Extensions\"; runOnlyForDeploymentPostprocessing = 0;")

# Configuraciones
def config(id_, nombre, ajustes):
    cuerpo = "isa = XCBuildConfiguration; buildSettings = {\n" + "".join(f"\t\t\t\t{k} = {v};\n" for k, v in sorted(ajustes.items())) + f"\t\t\t}}; name = {nombre};"
    obj(id_, nombre, cuerpo)


comunes = {
    "ALWAYS_SEARCH_USER_PATHS": "NO",
    "CLANG_ENABLE_MODULES": "YES",
    "CLANG_ENABLE_OBJC_ARC": "YES",
    "ENABLE_USER_SCRIPT_SANDBOXING": "NO",
    "IPHONEOS_DEPLOYMENT_TARGET": DESPLIEGUE,
    "SDKROOT": "iphoneos",
    "SWIFT_VERSION": "5.0",
}
p_debug, p_release = uid("proy-debug"), uid("proy-release")
config(p_debug, "Debug", {**comunes, "DEBUG_INFORMATION_FORMAT": "dwarf", "ENABLE_TESTABILITY": "YES", "GCC_OPTIMIZATION_LEVEL": "0",
                          "ONLY_ACTIVE_ARCH": "YES", "SWIFT_ACTIVE_COMPILATION_CONDITIONS": "DEBUG", "SWIFT_OPTIMIZATION_LEVEL": q("-Onone")})
config(p_release, "Release", {**comunes, "DEBUG_INFORMATION_FORMAT": q("dwarf-with-dsym"), "SWIFT_COMPILATION_MODE": "wholemodule", "VALIDATE_PRODUCT": "YES"})
t_comunes = {
    "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
    "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME": "AccentColor",
    "CODE_SIGN_ENTITLEMENTS": f"{NOMBRE}/{NOMBRE}.entitlements",
    "CODE_SIGN_STYLE": "Automatic",
    "CURRENT_PROJECT_VERSION": "1",
    "GENERATE_INFOPLIST_FILE": "NO",
    "INFOPLIST_FILE": f"{NOMBRE}/Info.plist",
    "LD_RUNPATH_SEARCH_PATHS": q("$(inherited) @executable_path/Frameworks"),
    "MARKETING_VERSION": "0.1.0",
    "PRODUCT_BUNDLE_IDENTIFIER": BUNDLE,
    "PRODUCT_NAME": q("$(TARGET_NAME)"),
    "SUPPORTED_PLATFORMS": q("iphoneos iphonesimulator"),
    "SUPPORTS_MACCATALYST": "NO",
    "SWIFT_EMIT_LOC_STRINGS": "YES",
    "TARGETED_DEVICE_FAMILY": "1",
}
c_debug, c_release = uid("target-debug"), uid("target-release")
config(c_debug, "Debug", t_comunes)
config(c_release, "Release", t_comunes)
t_ext = {
    **t_comunes,
    "APPLICATION_EXTENSION_API_ONLY": "YES",
    "CODE_SIGN_ENTITLEMENTS": f"{EXT}/{EXT}.entitlements",
    "INFOPLIST_FILE": f"{EXT}/Info.plist",
    "LD_RUNPATH_SEARCH_PATHS": q("$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks"),
    "PRODUCT_BUNDLE_IDENTIFIER": BUNDLE_EXT,
    "SKIP_INSTALL": "YES",
}
for clave in ("ASSETCATALOG_COMPILER_APPICON_NAME", "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME"):
    t_ext.pop(clave)
ce_debug, ce_release = uid("ext-debug"), uid("ext-release")
config(ce_debug, "Debug", t_ext)
config(ce_release, "Release", t_ext)
l_proy, l_target = uid("lista-proyecto"), uid("lista-target")
obj(l_proy, f'Build configuration list for PBXProject "{NOMBRE}"', f"isa = XCConfigurationList; buildConfigurations = (\n\t\t\t\t{p_debug},\n\t\t\t\t{p_release},\n\t\t\t); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release;")
obj(l_target, f'Build configuration list for PBXNativeTarget "{NOMBRE}"', f"isa = XCConfigurationList; buildConfigurations = (\n\t\t\t\t{c_debug},\n\t\t\t\t{c_release},\n\t\t\t); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release;")
l_ext = uid("lista-ext")
obj(l_ext, f'Build configuration list for PBXNativeTarget "{EXT}"', f"isa = XCConfigurationList; buildConfigurations = (\n\t\t\t\t{ce_debug},\n\t\t\t\t{ce_release},\n\t\t\t); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release;")

# Target y proyecto
target = uid("target")
target_ext = uid("target-ext")
proyecto = uid("proyecto")
proxy = uid("proxy-ext")
obj(proxy, "PBXContainerItemProxy", f"isa = PBXContainerItemProxy; containerPortal = {proyecto} /* Project object */; proxyType = 1; remoteGlobalIDString = {target_ext}; remoteInfo = {EXT};")
dependencia = uid("dependencia-ext")
obj(dependencia, "PBXTargetDependency", f"isa = PBXTargetDependency; target = {target_ext}; targetProxy = {proxy};")
obj(target_ext, EXT, f"isa = PBXNativeTarget; buildConfigurationList = {l_ext}; buildPhases = (\n\t\t\t\t{f_fuentes_ext},\n\t\t\t\t{f_frameworks_ext},\n\t\t\t); buildRules = (\n\t\t\t); dependencies = (\n\t\t\t); name = {EXT}; productName = {EXT}; productReference = {ref_producto_ext}; productType = \"com.apple.product-type.app-extension\";")
obj(target, NOMBRE, f"isa = PBXNativeTarget; buildConfigurationList = {l_target}; buildPhases = (\n\t\t\t\t{f_fuentes},\n\t\t\t\t{f_frameworks},\n\t\t\t\t{f_recursos},\n\t\t\t\t{f_embed},\n\t\t\t); buildRules = (\n\t\t\t); dependencies = (\n\t\t\t\t{dependencia},\n\t\t\t); name = {NOMBRE}; productName = {NOMBRE}; productReference = {ref_producto}; productType = \"com.apple.product-type.application\";")
obj(proyecto, "Project object", f"isa = PBXProject; attributes = {{\n\t\t\t\tBuildIndependentTargetsInParallel = 1;\n\t\t\t\tLastSwiftUpdateCheck = 2700;\n\t\t\t\tLastUpgradeCheck = 2700;\n\t\t\t}}; buildConfigurationList = {l_proy}; compatibilityVersion = \"Xcode 14.0\"; developmentRegion = es; hasScannedForEncodings = 0; knownRegions = (\n\t\t\t\tes,\n\t\t\t\tBase,\n\t\t\t); mainGroup = {g_raiz}; productRefGroup = {g_productos}; projectDirPath = \"\"; projectRoot = \"\"; targets = (\n\t\t\t\t{target},\n\t\t\t\t{target_ext},\n\t\t\t);")

tipos = {"PBXBuildFile": [], "PBXContainerItemProxy": [], "PBXCopyFilesBuildPhase": [], "PBXFileReference": [], "PBXFrameworksBuildPhase": [], "PBXGroup": [], "PBXNativeTarget": [], "PBXProject": [],
         "PBXResourcesBuildPhase": [], "PBXSourcesBuildPhase": [], "PBXTargetDependency": [], "XCBuildConfiguration": [], "XCConfigurationList": []}
for id_, com, cuerpo in objetos:
    tipos[cuerpo.split(";")[0].split("=")[1].strip()].append((id_, com, cuerpo))

salida = ["// !$*UTF8*$!\n{\n\tarchiveVersion = 1;\n\tclasses = {\n\t};\n\tobjectVersion = 56;\n\tobjects = {\n"]
for tipo in sorted(tipos):
    if not tipos[tipo]:
        continue
    salida.append(f"\n/* Begin {tipo} section */\n")
    for id_, com, cuerpo in sorted(tipos[tipo]):
        etiqueta = f" /* {com} */" if com else ""
        salida.append(f"\t\t{id_}{etiqueta} = {{{cuerpo}}};\n")
    salida.append(f"/* End {tipo} section */\n")
salida.append(f"\t}};\n\trootObject = {proyecto} /* Project object */;\n}}\n")

proyecto_dir = os.path.join(RAIZ, f"{NOMBRE}.xcodeproj")
os.makedirs(os.path.join(proyecto_dir, "xcshareddata", "xcschemes"), exist_ok=True)
with open(os.path.join(proyecto_dir, "project.pbxproj"), "w") as f:
    f.write("".join(salida))
esquema = f"""<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion = "2700" version = "1.7">
   <BuildAction parallelizeBuildables = "YES" buildImplicitDependencies = "YES">
      <BuildActionEntries>
         <BuildActionEntry buildForTesting = "YES" buildForRunning = "YES" buildForProfiling = "YES" buildForArchiving = "YES" buildForAnalyzing = "YES">
            <BuildableReference BuildableIdentifier = "primary" BlueprintIdentifier = "{target}" BuildableName = "{NOMBRE}.app" BlueprintName = "{NOMBRE}" ReferencedContainer = "container:{NOMBRE}.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction buildConfiguration = "Debug" selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv = "YES">
   </TestAction>
   <LaunchAction buildConfiguration = "Debug" selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB" launchStyle = "0" useCustomWorkingDirectory = "NO" ignoresPersistentStateOnLaunch = "NO" debugDocumentVersioning = "YES" debugServiceExtension = "internal" allowLocationSimulation = "YES">
      <BuildableProductRunnable runnableDebuggingMode = "0">
         <BuildableReference BuildableIdentifier = "primary" BlueprintIdentifier = "{target}" BuildableName = "{NOMBRE}.app" BlueprintName = "{NOMBRE}" ReferencedContainer = "container:{NOMBRE}.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction buildConfiguration = "Release" shouldUseLaunchSchemeArgsEnv = "YES" savedToolIdentifier = "" useCustomWorkingDirectory = "NO" debugDocumentVersioning = "YES">
   </ProfileAction>
   <AnalyzeAction buildConfiguration = "Debug">
   </AnalyzeAction>
   <ArchiveAction buildConfiguration = "Release" revealArchiveInOrganizer = "YES">
   </ArchiveAction>
</Scheme>
"""
with open(os.path.join(proyecto_dir, "xcshareddata", "xcschemes", f"{NOMBRE}.xcscheme"), "w") as f:
    f.write(esquema)
print("Proyecto generado:", os.path.relpath(proyecto_dir), "|", len(fuentes), "archivos Swift,", len(recursos), "recursos")
