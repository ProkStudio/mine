from pathlib import Path
import hashlib, shutil

root=Path('.')
def sha(content):
    b=content.encode('utf-8');return hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()
def edit(path,expected,changes):
    p=root/path;s=p.read_text(encoding='utf-8')
    if sha(s)!=expected: raise RuntimeError('Source changed; refusing overwrite: '+path)
    for old,new,count in changes:
        if s.count(old)!=count: raise RuntimeError(f'{path}: expected {count} occurrences of {old!r}, got {s.count(old)}')
        s=s.replace(old,new)
    p.write_text(s,encoding='utf-8')
V='src/main/java/com/harvester/vehicle/'
C='src/client/java/com/harvester/client/'
edit(V+'VehicleType.java','efb756a5abf7fbd8a5b4c59eaa5383c1cc478bf5',[
 ('    public enum Family','    /** One shared world scale; UVs and blueprint dimensions stay unchanged. */\n    public static final float MODEL_SCALE=1.125f;\n    public enum Family',1),
 ('public final float width, height;','public final float width, height, blueprintWidth, blueprintHeight;',1),
 ('this.width=width; this.height=height;','this.blueprintWidth=width; this.blueprintHeight=height;\n        this.width=width*MODEL_SCALE; this.height=height*MODEL_SCALE;',1)])
edit(V+'VehicleGeometry.java','fa44ed4664c7c650decc9e8d03624894becfc900',[
 ('public static List<Part> create(VehicleType type) {','public static List<Part> create(VehicleType type) {\n        return VehicleAssembly.complete(type,blueprint(type));\n    }\n    private static List<Part> blueprint(VehicleType type) {',1),('type.width','type.blueprintWidth',4)])
edit(V+'VehicleDetailing.java','06160222e3e6e1403042c3df9ca6c315859f7fb4',[('type.width','type.blueprintWidth',6)])
edit(V+'VehicleRig.java','fc0b8b41d647725b26a08702317ae9dcfa08b4d9',[
 ('return new Point(px+x*cr-yy*sr,py+x*sr+yy*cr,pz+zz);','double scale=VehicleType.MODEL_SCALE;\n        return new Point((px+x*cr-yy*sr)*scale,(py+x*sr+yy*cr)*scale,(pz+zz)*scale);',1)])
edit(V+'PassengerAnimation.java','3f1c661e0d81f6d88c28fd1dbadf8fdca8ba24b1',[
 ('private static Joint arm(int side,Grip grip,double pitch,double roll) {','private static Joint arm(int side,Grip grip,double pitch,double roll) {\n        grip=new Grip(grip.x()*VehicleType.MODEL_SCALE,grip.y()*VehicleType.MODEL_SCALE,grip.z()*VehicleType.MODEL_SCALE);',1)])
edit(V+'VehicleEffects.java','232b2a954f78ee643b87ea52258cffbadd3c79c6',[
 ('type.width','type.blueprintWidth',2),('double rear=type==VehicleType.BOAT_CARGO?-1.48:-1.30;','double rear=-VehicleGeometry.boatLength(type)/32.0;',1),
 ('vehicle.localEffect(.73,.43,type==VehicleType.PICKUP_CARGO?-1.25:-1.10)','vehicle.localEffect(-8.25/16,5.25/16,(type==VehicleType.PICKUP_CARGO?-19.8:-17.3)/16)',1),
 ('vehicle.localEffect(.39,.62,type==VehicleType.PLANE_CARGO?1.62:1.31)','vehicle.localEffect(.45,.61,.62)',1)])
edit(C+'entity/renderer/CombineRenderState.java','8f7abeb4ee9f0e0dc4d4b6f57a98182d8b1cf34a',[
 ('import com.harvester.vehicle.VehiclePresentation;','import com.harvester.vehicle.VehiclePresentation;\nimport com.harvester.vehicle.VehicleMechanics;',1),
 ('public Map<String,Float> suspension=Map.of();','public Map<String,Float> suspension=Map.of();\n    public VehicleMechanics.Frame mechanics=VehicleMechanics.Frame.REST;',1)])
edit(C+'entity/renderer/CombineRenderer.java','7839cf8910320a70a08b90f0085320d64f6831fd',[
 ('import net.minecraft.client.render.OverlayTexture;','import net.minecraft.client.render.OverlayTexture;\nimport net.minecraft.client.render.Frustum;',1),
 ('final VehiclePresentation presentation=new VehiclePresentation();','final VehiclePresentation presentation=new VehiclePresentation();\n        final VehicleMechanics mechanics=new VehicleMechanics();',1),
 ('private final Map<CombineEntity,History> histories','private final Map<VehicleType,Double> visibilityRadius=new EnumMap<>(VehicleType.class);\n    private final Map<CombineEntity,History> histories',1),
 ('double radius=p.name().startsWith("wheel")?VehicleAnimation.wheelRadius(p,definitions):1;','double radius=(p.name().startsWith("wheel")?VehicleAnimation.wheelRadius(p,definitions):p.name().equals("rear_sprocket")?5.5/16:1)*VehicleType.MODEL_SCALE;',1),
 ('models.put(type,List.copyOf(parts));','models.put(type,List.copyOf(parts));\n            visibilityRadius.put(type,VehicleMechanics.renderRadius(type,definitions));',1),
 ('    @Override public CombineRenderState createRenderState()', '    @Override public boolean shouldRender(CombineEntity entity,Frustum frustum,double x,double y,double z) {\n        return entity.shouldRender(x,y,z) && frustum.isVisible(entity.getBoundingBox().expand(visibilityRadius.getOrDefault(entity.variant(),4.0)));\n    }\n    @Override public CombineRenderState createRenderState()',1),
 ('entity.getCondition()/(double)Math.max(1,entity.stats().durability)));','entity.getCondition()/(double)Math.max(1,entity.stats().durability)));\n        state.mechanics=h.mechanics.update(state.animationTime,state.variant,entity.getEntityWorld().isRaining(),entity.hasPassengers(),state.presentation.rpm(),state.inputDrive,h.speed);',1),
 ('if(entity.isOnGround()) {','if(entity.isOnGround() && (state.variant.family==VehicleType.Family.PICKUP || state.variant.family==VehicleType.Family.COMBINE)) {',1),
 ('matrices.push(); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));','matrices.push(); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));\n        matrices.scale(VehicleType.MODEL_SCALE,VehicleType.MODEL_SCALE,VehicleType.MODEL_SCALE);',1),
 ('name.equals("reel")','name.startsWith("reel")',2),('wheel=name.startsWith("wheel");','wheel=name.startsWith("wheel") || name.equals("rear_sprocket");',1),
 ('state.yawTravel*def.px()/16','state.yawTravel*def.px()/16*VehicleType.MODEL_SCALE',1),('travel*16);','travel*16/VehicleType.MODEL_SCALE);',1),
 ('at.y()/16+trackSpring(state,fields[1])','at.y()/16+trackSpring(state,fields[1])/VehicleType.MODEL_SCALE',1),
 ('/16.0+spring,def.pz()/16.0);','/16.0+spring/VehicleType.MODEL_SCALE,def.pz()/16.0);\n                if(state.variant.family==VehicleType.Family.HELICOPTER && name.startsWith("rotor")) {\n                    matrices.multiply(RotationAxis.POSITIVE_Z.rotation(state.presentation.swashRoll()));\n                    matrices.multiply(RotationAxis.POSITIVE_X.rotation(state.presentation.swashPitch()));\n                }',1),
 ("                    case 'x' ->", "                    case 'd' -> {\n                        var link=VehicleMechanics.hydraulic(name.startsWith(\"hydraulic_header\"),state.headerLift,state.rotor,state.workingStrength);\n                        matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)link.deltaPitch()));\n                        if(name.contains(\"_piston_\")) {\n                            matrices.translate(0,0,link.pistonOffset()/16);\n                            matrices.scale(1,1,(float)link.pistonScale());\n                            matrices.translate(0,0,-link.pistonOffset()/16);\n                        }\n                    }\n                    case 'w' -> matrices.multiply(RotationAxis.POSITIVE_Z.rotation(state.mechanics.wiper()*(name.endsWith(\"_-1\")?-1:1)));\n                    case 'q' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation(-(name.equals(\"pedal_drive\")?state.mechanics.accelerator()*.20f:state.mechanics.brake()*.28f)));\n                    case 's' -> matrices.scale(1,VehicleMechanics.springScale(state.suspension.getOrDefault(\"wheel_\"+name.substring(12),0f)),1);\n                    case 'f' -> matrices.multiply(RotationAxis.POSITIVE_Z.rotation(VehicleAnimation.wheelPhase(travel,5.0/16*VehicleType.MODEL_SCALE)*2.2f));\n                    case 'x' ->",1),
 ('float inverseDensity=1f/VehicleAtlas.DENSITY;', 'boolean signal=name.startsWith("signal_");\n            boolean lit=name.startsWith("signal_brake")?state.mechanics.brakeLamp():name.startsWith("signal_reverse")?state.mechanics.reverseLamp():def.material().equals("lamp") && state.engineActive;\n            if(signal && !lit) tint=0xff555555;\n            int light=lit?0x00f000f0:state.light;\n            float inverseDensity=1f/VehicleAtlas.DENSITY;',1),
 ('part.texture()),state.light,OverlayTexture','part.texture()),light,OverlayTexture',1)])
edit(C+'sound/VehicleEngineSound.java','4ae3f191ff79c47963cd90a9761691dc9e77536d',[
 ('import com.harvester.init.ModSounds;','import com.harvester.init.ModSounds;\nimport com.harvester.vehicle.VehicleSoundEnvelope;',1),
 ('private final float gain;','private final float gain;\n    private final VehicleSoundEnvelope envelope=new VehicleSoundEnvelope();',1),
 ('        float load=(float)Math.clamp(Math.abs(vehicle.driveInput())*.6+(Double.isFinite(speed)?speed/.5:0)*.25+(vehicle.isWorking()?.15:0),0,1);\n        float targetVolume=powered?gain*(.35f+.35f*load):0;\n        volume+=(targetVolume-volume)*.25f; pitch+=(.75f+.45f*load-pitch)*.15f;', '        var frame=envelope.update(vehicle.variant(),1,powered,vehicle.driveInput(),speed,vehicle.isWorking(),gain);\n        volume=frame.volume();pitch=frame.pitch();',1)])
edit(C+'sound/VehicleAudio.java','171c6df2406a23e02fd8bb03372f7d00c4a4d0c9',[
 ('private record Loop(VehicleEngineSound sound,long started) {}','private record Loop(VehicleEngineSound sound,long started,com.harvester.vehicle.VehicleType type) {}',1),
 ('nearby.sort(Comparator.comparingDouble(v->client.player.squaredDistanceTo(v)));','nearby.sort(Comparator.comparingDouble((CombineEntity v)->client.player.getVehicle()==v?-1:client.player.squaredDistanceTo(v)-(SOUNDS.containsKey(v)?4:0)));',1),
 ('if(lost || sound.finished() || unavailable)','if(lost || sound.finished() || unavailable || loop.type()!=vehicle.variant())',1),('new Loop(sound,clock)','new Loop(sound,clock,vehicle.variant())',1)])
edit('src/test/java/com/harvester/vehicle/VehicleVisualResourcesTest.java','7e1108f10ed2c3a0f8bb280bd1dd6ad24a803900',[
 ('assertEquals(16000,','assertEquals(32000,',2),('assertEquals(32000,metadata.get("samples")','assertEquals(128000,metadata.get("samples")',1)])
edit('build.gradle','8ce3a294235ea5a7c030725ee58cc477298c72f6',[
 ("        'src/main/java/com/harvester/vehicle/VehicleType.java')", "        'src/main/java/com/harvester/vehicle/VehicleType.java',\n        'src/main/java/com/harvester/vehicle/VehicleAssembly.java',\n        'src/main/java/com/harvester/vehicle/VehicleMechanics.java',\n        'src/main/java/com/harvester/vehicle/VehicleSoundEnvelope.java',\n        'tools/src/VehiclePolishSmoke.java')",1),
 ('check.dependsOn transportRules, validateVehicleResources',"tasks.register('transportPolishSmoke', JavaExec) {\n    dependsOn compileVehicleArt\n    classpath = files(artClasses)\n    mainClass = 'VehiclePolishSmoke'\n    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }\n    args layout.buildDirectory.dir('polish-meshes').get().asFile.absolutePath\n}\ntasks.withType(AbstractArchiveTask).configureEach { preserveFileTimestamps=false; reproducibleFileOrder=true }\ncheck.dependsOn transportRules, validateVehicleResources, transportPolishSmoke",1)])
edit('tests/run-visual-smoke.sh','e4b8f1c3990f13aa13d48f841f83e85ef4258615',[( '"$src/VehicleAtlas.java"','"$src/VehicleAssembly.java" "$src/VehicleAtlas.java"',1)])
edit('gradle.properties','9bff15fedbccc7a042bb65d102b5f593967ee7e6',[('version=1.2.0','version=1.3.0-rc.1',1)])
edit('tests/verify_results.py','cda56a586280043ac9f8250024fa30a9f119d50f',[
 ("EXPECTED = {", "EXPECTED = {\n    'VehicleReleasePolishTest': {'scaleAppliedExactlyOnce', 'hydraulicEndpointsStayAttached', 'wipersReturnToPark', 'signalsUseActualDirection', 'audioEnvelopeSettlesAndIsFinite', 'everyFamilyHasMechanicalRoots'},",1),
 ("OUTPUTS = {", "OUTPUTS = {\n    'VehicleReleasePolishTest': 'vehicle-release-polish-tests.json',",1)])
edit('README.md','06202518488bbf7a56a740a52fbb91a91cc13a5a',[
 ('# Harvester Mod — Transport Pack 1.2.0','# Harvester Mod — Transport Pack 1.3.0-rc.1\n\n> **Кандидат в релиз, не stable.** Новый масштаб ×1.125, механические крепления, дворники/педали/штоки, сигналы и переработанное mono-аудио 32 kHz / 4 s. Ожидается 59 JUnit-методов и новый polish-smoke. Фактические текущие результаты — в [рабочем конспекте](docs/TRANSPORT-RELEASE-STATE.md). Приведённый ниже PASS старого коммита относится только к прежней итерации. Minecraft-клиентская приёмка ещё требуется.',1)])
inputs=Path('tools/polish-input')
for name in ['VehicleAssembly','VehicleMechanics','VehicleSoundEnvelope']:
    destination=Path(V+name+'.java')
    if destination.exists(): raise RuntimeError('Unexpected existing source: '+str(destination))
    shutil.copyfile(inputs/(name+'.java'),destination)
if sha(Path('tools/src/VehicleAudioGenerator.java').read_text())!='e207d69e8814de56a688fde2a1ebeccc0baeac6c': raise RuntimeError('Audio source changed')
shutil.copyfile(inputs/'VehicleAudioGenerator.java','tools/src/VehicleAudioGenerator.java')
for source,destination in [('VehiclePolishSmoke.java','tools/src/VehiclePolishSmoke.java'),('VehicleReleasePolishTest.java','src/test/java/com/harvester/vehicle/VehicleReleasePolishTest.java')]:
    if Path(destination).exists(): raise RuntimeError('Unexpected existing source: '+destination)
    shutil.copyfile(inputs/source,destination)
state=Path('docs/TRANSPORT-RELEASE-STATE.md')
state.write_text('# Transport 1.3.0-rc.1 — интеграционный checkpoint\n\nИсходники из staging интегрированы в стандартные src/ и tools/src/. Временные файлы удаляются в этом коммите. Масштаб 1.125, механические крепления всех семейств, дополнительные движения и новое аудио подключены к рендеру/посадке/эффектам.\n\nЛокально: polish-smoke PASS для 16 вариантов, 583100 assertions; 6 mono OGG 32 kHz / 4 s декодированы и повторно получены byte-for-byte тем же encoder. Это НЕ клиентский запуск.\n\nТекущий workflow обязан выполнить clean build клиента/Fabric, 59 обязательных JUnit-методов, оба автономных smoke и сверку экспортов до push коммита. Смотреть фактический результат CI; не переносить старый PASS на новый код. После успеха — собрать ZIP/JAR/SHA-256 и выпустить prerelease v1.3.0-rc.1. Стабильный релиз остаётся заблокирован до Minecraft QA: посадка/броня, камера, столкновения/вода, resource reload, несколько машин, прослушивание, FPS.\n\nПродолжение: прочитать актуальный HEAD и проверки PR #5; при ошибке — docs/TRANSPORT-CI-LAST.log. Обновить этот конспект реальной ссылкой CI и релиза после их появления.\n',encoding='utf-8')
print('Applied all source-checked integration edits')
