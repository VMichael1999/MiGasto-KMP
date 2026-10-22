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


# Extensiones de la app. Cada una vive en su carpeta de iosApp, se embebe en la app y se compila aparte:
#  - Compartir: lee con Vision las capturas que el usuario comparte y las deja en una cola de la app.
#  - Widgets: el widget de pantalla de inicio y el control del Centro de control que abren «Nuevo movimiento».
EXTENSIONES = [
    # `compartidas`: archivos de la app que también se compilan dentro de la extensión.
    {"nombre": "MiGastoCompartir", "sufijo": "compartir", "entitlements": True,
     "compartidas": [os.path.join(NOMBRE, "Compartido", "ColaDeCapturas.swift")]},
    {"nombre": "MiGastoWidgets", "sufijo": "widgets", "entitlements": False, "compartidas": []},
]

fuentes = archivos(NOMBRE, (".swift",))
recursos = archivos(os.path.join(NOMBRE, "Resources"), (".ttf", ".otf"))

objetos = []  # (id, comentario, cuerpo)


def obj(id_, comentario, cuerpo):
    objetos.append((id_, comentario, cuerpo))


def q(s):
    return '"' + s.replace('"', '\\"') + '"'


def lista(ids):
    return "".join(f"\t\t\t\t{i},\n" for i in ids)


# Referencias a archivos de la app
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
ref_producto = uid("ref-producto")
obj(ref_producto, f"{NOMBRE}.app", f"isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = {NOMBRE}.app; sourceTree = BUILT_PRODUCTS_DIR;")

# Archivos de compilación de la app
build = {}
for ruta in fuentes + recursos:
    build[ruta] = uid("build" + ruta)
    obj(build[ruta], f"{os.path.basename(ruta)} in " + ("Sources" if ruta in fuentes else "Resources"), f"isa = PBXBuildFile; fileRef = {refs[ruta]};")
build_assets = uid("build-assets")
obj(build_assets, "Assets.xcassets in Resources", f"isa = PBXBuildFile; fileRef = {ref_assets};")

# Grupos de la app
g_resources = uid("grupo-resources")
obj(g_resources, "Resources", "isa = PBXGroup; children = (\n" + lista([refs[r] for r in recursos]) + "\t\t\t); path = Resources; sourceTree = \"<group>\";")
g_app = uid("grupo-app")
raiz_swift = [r for r in fuentes if os.path.dirname(r) == NOMBRE]
subcarpetas = sorted({os.path.dirname(r) for r in fuentes if os.path.dirname(r) != NOMBRE})
g_sub = {}
for sc in subcarpetas:
    g_sub[sc] = uid("grupo" + sc)
    hijos = [r for r in fuentes if os.path.dirname(r) == sc]
    obj(g_sub[sc], os.path.basename(sc), "isa = PBXGroup; children = (\n" + lista([refs[r] for r in hijos]) + f"\t\t\t); path = {q(os.path.basename(sc))}; sourceTree = \"<group>\";")
hijos_app = [refs[r] for r in raiz_swift] + list(g_sub.values()) + [g_resources, ref_assets, ref_info, ref_ent_app]
obj(g_app, NOMBRE, "isa = PBXGroup; children = (\n" + lista(hijos_app) + f"\t\t\t); path = {NOMBRE}; sourceTree = \"<group>\";")

# Fases de la app
f_fuentes, f_frameworks, f_recursos, f_embed = uid("fase-fuentes"), uid("fase-frameworks"), uid("fase-recursos"), uid("fase-embed")
obj(f_fuentes, "Sources", "isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = (\n" + lista([build[r] for r in fuentes]) + "\t\t\t); runOnlyForDeploymentPostprocessing = 0;")
obj(f_frameworks, "Frameworks", "isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (\n\t\t\t); runOnlyForDeploymentPostprocessing = 0;")
obj(f_recursos, "Resources", "isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = (\n" + lista([build[r] for r in recursos] + [build_assets]) + "\t\t\t); runOnlyForDeploymentPostprocessing = 0;")


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


def lista_de_configuraciones(id_, nombre_del_target, debug, release):
    obj(id_, f'Build configuration list for PBXNativeTarget "{nombre_del_target}"',
        f"isa = XCConfigurationList; buildConfigurations = (\n\t\t\t\t{debug},\n\t\t\t\t{release},\n\t\t\t); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release;")


c_debug, c_release = uid("target-debug"), uid("target-release")
config(c_debug, "Debug", t_comunes)
config(c_release, "Release", t_comunes)
l_proy, l_target = uid("lista-proyecto"), uid("lista-target")
obj(l_proy, f'Build configuration list for PBXProject "{NOMBRE}"', f"isa = XCConfigurationList; buildConfigurations = (\n\t\t\t\t{p_debug},\n\t\t\t\t{p_release},\n\t\t\t); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release;")
lista_de_configuraciones(l_target, NOMBRE, c_debug, c_release)

target = uid("target")
proyecto = uid("proyecto")

# Las extensiones
grupos_ext, productos_ext, embebidos, dependencias, targets_ext = [], [], [], [], []
for ext in EXTENSIONES:
    nombre = ext["nombre"]
    clave = ext["sufijo"]
    fuentes_ext = archivos(nombre, (".swift",))
    for ruta in fuentes_ext:
        refs[ruta] = uid("ref" + ruta)
        obj(refs[ruta], os.path.basename(ruta), f"isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {q(os.path.basename(ruta))}; sourceTree = \"<group>\";")
    ref_info_ext = uid(f"ref-info-{clave}")
    obj(ref_info_ext, "Info.plist", "isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = \"<group>\";")
    hijos_ext = [refs[r] for r in fuentes_ext] + [ref_info_ext]
    if ext["entitlements"]:
        ref_ent = uid(f"ref-ent-{clave}")
        obj(ref_ent, f"{nombre}.entitlements", f"isa = PBXFileReference; lastKnownFileType = text.plist.entitlements; path = {nombre}.entitlements; sourceTree = \"<group>\";")
        hijos_ext.append(ref_ent)
    ref_prod = uid(f"ref-producto-{clave}")
    obj(ref_prod, f"{nombre}.appex", f"isa = PBXFileReference; explicitFileType = \"wrapper.app-extension\"; includeInIndex = 0; path = {nombre}.appex; sourceTree = BUILT_PRODUCTS_DIR;")
    grupo = uid(f"grupo-{clave}")
    obj(grupo, nombre, "isa = PBXGroup; children = (\n" + lista(hijos_ext) + f"\t\t\t); path = {nombre}; sourceTree = \"<group>\";")

    build_ext = {}
    for ruta in fuentes_ext + ext["compartidas"]:
        build_ext[ruta] = uid(f"buildext-{clave}" + ruta)
        obj(build_ext[ruta], f"{os.path.basename(ruta)} in Sources", f"isa = PBXBuildFile; fileRef = {refs[ruta]};")
    build_embed = uid(f"build-embed-{clave}")
    obj(build_embed, f"{nombre}.appex in Embed Foundation Extensions", f"isa = PBXBuildFile; fileRef = {ref_prod}; settings = {{ATTRIBUTES = (RemoveHeadersOnCopy, ); }};")
    f_src, f_frw = uid(f"fase-fuentes-{clave}"), uid(f"fase-frameworks-{clave}")
    obj(f_src, "Sources", "isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = (\n" + lista([build_ext[r] for r in fuentes_ext + ext["compartidas"]]) + "\t\t\t); runOnlyForDeploymentPostprocessing = 0;")
    obj(f_frw, "Frameworks", "isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (\n\t\t\t); runOnlyForDeploymentPostprocessing = 0;")

    t_ext = {
        **t_comunes,
        "APPLICATION_EXTENSION_API_ONLY": "YES",
        "INFOPLIST_FILE": f"{nombre}/Info.plist",
        "LD_RUNPATH_SEARCH_PATHS": q("$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks"),
        "PRODUCT_BUNDLE_IDENTIFIER": f"{BUNDLE}.{clave}",
        "SKIP_INSTALL": "YES",
    }
    for k in ("ASSETCATALOG_COMPILER_APPICON_NAME", "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME"):
        t_ext.pop(k)
    if ext["entitlements"]:
        t_ext["CODE_SIGN_ENTITLEMENTS"] = f"{nombre}/{nombre}.entitlements"
    else:
        t_ext.pop("CODE_SIGN_ENTITLEMENTS")
    ce_debug, ce_release = uid(f"ext-debug-{clave}"), uid(f"ext-release-{clave}")
    config(ce_debug, "Debug", t_ext)
    config(ce_release, "Release", t_ext)
    l_ext = uid(f"lista-{clave}")
    lista_de_configuraciones(l_ext, nombre, ce_debug, ce_release)

    target_ext = uid(f"target-{clave}")
    proxy = uid(f"proxy-{clave}")
    obj(proxy, "PBXContainerItemProxy", f"isa = PBXContainerItemProxy; containerPortal = {proyecto} /* Project object */; proxyType = 1; remoteGlobalIDString = {target_ext}; remoteInfo = {nombre};")
    dependencia = uid(f"dependencia-{clave}")
    obj(dependencia, "PBXTargetDependency", f"isa = PBXTargetDependency; target = {target_ext}; targetProxy = {proxy};")
    obj(target_ext, nombre, f"isa = PBXNativeTarget; buildConfigurationList = {l_ext}; buildPhases = (\n\t\t\t\t{f_src},\n\t\t\t\t{f_frw},\n\t\t\t); buildRules = (\n\t\t\t); dependencies = (\n\t\t\t); name = {nombre}; productName = {nombre}; productReference = {ref_prod}; productType = \"com.apple.product-type.app-extension\";")
    grupos_ext.append(grupo)
    productos_ext.append(ref_prod)
    embebidos.append(build_embed)
    dependencias.append(dependencia)
    targets_ext.append(target_ext)

obj(f_embed, "Embed Foundation Extensions", f"isa = PBXCopyFilesBuildPhase; buildActionMask = 2147483647; dstPath = \"\"; dstSubfolderSpec = 13; files = (\n{lista(embebidos)}\t\t\t); name = \"Embed Foundation Extensions\"; runOnlyForDeploymentPostprocessing = 0;")

# Productos y grupo raíz
g_productos = uid("grupo-productos")
obj(g_productos, "Products", f"isa = PBXGroup; children = (\n{lista([ref_producto] + productos_ext)}\t\t\t); name = Products; sourceTree = \"<group>\";")
g_raiz = uid("grupo-raiz")
obj(g_raiz, "", f"isa = PBXGroup; children = (\n{lista([g_app] + grupos_ext + [g_productos])}\t\t\t); sourceTree = \"<group>\";")

# Target de la app y proyecto
obj(target, NOMBRE, f"isa = PBXNativeTarget; buildConfigurationList = {l_target}; buildPhases = (\n{lista([f_fuentes, f_frameworks, f_recursos, f_embed])}\t\t\t); buildRules = (\n\t\t\t); dependencies = (\n{lista(dependencias)}\t\t\t); name = {NOMBRE}; productName = {NOMBRE}; productReference = {ref_producto}; productType = \"com.apple.product-type.application\";")
obj(proyecto, "Project object", f"isa = PBXProject; attributes = {{\n\t\t\t\tBuildIndependentTargetsInParallel = 1;\n\t\t\t\tLastSwiftUpdateCheck = 2700;\n\t\t\t\tLastUpgradeCheck = 2700;\n\t\t\t}}; buildConfigurationList = {l_proy}; compatibilityVersion = \"Xcode 14.0\"; developmentRegion = es; hasScannedForEncodings = 0; knownRegions = (\n\t\t\t\tes,\n\t\t\t\tBase,\n\t\t\t); mainGroup = {g_raiz}; productRefGroup = {g_productos}; projectDirPath = \"\"; projectRoot = \"\"; targets = (\n{lista([target] + targets_ext)}\t\t\t);")

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
