#!/usr/bin/env python3
"""Authored plant matrix -> registration, native recipes, profiles and retinted donor models.
Run with --check for byte-for-byte reproducibility. No caches or historical audits are inputs.
"""
import argparse, colorsys, hashlib, io, json, pathlib
from PIL import Image
ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
NS = 'better_drinking_water:'
rows, coverage = [], []
old = json.loads((RES/'drinks.json').read_text())
profiles = {r['id']:r for r in old['drinks'] if not r['id'].startswith(NS)}
def effect(name, duration=1200, amp=0, chance=1):
    return dict(id=name if ':' in name else 'minecraft:'+name, duration=duration, amplifier=amp, chance=chance)
def add(name, ingredient=None, count=1, colour='aa7733', group='fruits', effects=(), action='none', base=None, agent=None, native=None, clone=None, process='juice', food=1, unsafe=False):
    item = native or NS+name
    fx=list(effects)
    hydration, quenching = (4, 5) if process == 'infusion' else (3, 4)
    if clone:
        source=profiles[clone]; fx=source['effects']; action=source['action']
        hydration,quenching=source['hydration'],source['quenching']
    nutrition={group:.025} if group else {}
    if group=='fruits': nutrition['sugar']=.0125
    if name=='cane_juice': nutrition={'vegetables':.025,'sugar':.0125}
    mods={s.split(':')[0] for s in [ingredient or 'minecraft:air',item]+[e['id'] for e in fx] if s.split(':')[0] not in ['minecraft','better_drinking_water']}
    if action=='mana': mods.add('ars_nouveau')
    if action in ['orange','lemon']: mods.add('fruitsdelight')
    row=dict(name=name,item=item,ingredient=ingredient,count=count,colour=colour,food=food,nutrition=nutrition,
             mods=sorted(mods),process=process,base=base,agent=agent,unsafe=unsafe,native_profile=clone)
    rows.append(row)
    if not native:
        profiles[item]=dict(id=item,hydration=hydration,quenching=quenching,remainder='minecraft:glass_bottle',effects=fx,action=action,
            source='Plant matrix v1; 250 mB; authored conservative dose, native specialist profile retained where indicated')
    return name

def fruit(plant,ingredient,count,colour,brew,fx=(),action='none',native=None,clone=None,agent=None):
    juice=add(plant+'_juice',ingredient,count,colour)
    specialty=add(brew,colour=colour,effects=fx,action=action,base=juice,agent=agent or '#better_drinking_water:safe_starter_flowers',native=native,clone=clone,process='primary')
    coverage.append(dict(plant=plant,ingredient=ingredient,portion=count,plain=NS+juice,brew=native or NS+specialty))

fruit('apple','minecraft:apple',1,'ffb51b','apple_cider',native='farmersdelight:apple_cider')
fruit('pear','fruitsdelight:pear',1,'b9f52b','orchard_soother',clone='fruitsdelight:pear_juice')
fruit('orange','fruitsdelight:orange',1,'ff7c08','suncrest_tonic',[effect('regeneration',200)],'orange')
fruit('kiwi','fruitsdelight:kiwi',1,'31df37','greenbound_fizz',[effect('jump_boost')])
for plant,colour,brew in [('mango','ee9b08','mango_tea'),('lychee','ffc5bd','lychee_cherry_tea'),('peach','ff706c','peach_tea'),('mangosteen','a94ce4','mangosteen_tea'),('hawberry','db1833','hawberry_tea')]:
    fruit(plant,'fruitsdelight:'+plant,2 if plant in ['lychee','hawberry'] else 1,colour,brew,native='fruitsdelight:'+brew)
fruit('bayberry','fruitsdelight:bayberry',2,'8f153d','duskberry_cordial',[effect('night_vision')])
fruit('fig','fruitsdelight:fig',1,'bf548e','featherfall_nectar',[effect('slow_falling')])
fruit('persimmon','fruitsdelight:persimmon',1,'d76818','embergold_tonic',[effect('fire_resistance')])
fruit('durian','fruitsdelight:durian_flesh',1,'c3dc19','thornhide_brew',[effect('resistance'),effect('nausea',100)])
fruit('blueberry','fruitsdelight:blueberry',2,'4934c9','bluehour_cordial',[effect('night_vision')])
fruit('cranberry','fruitsdelight:cranberry',2,'ed194b','bogclear_tonic',action='poison')
fruit('sweet_berry','minecraft:sweet_berries',2,'db2972','bramblefoot_fizz',[effect('speed')])
fruit('glow_berry','minecraft:glow_berries',2,'ffcf2e','glittering_grenadine',native='brewinandchewin:glittering_grenadine')
fruit('mulberry','upgrade_aquatic:mulberry',2,'4d125d','blackbough_cordial',[effect('strength')])
fruit('sourceberry','ars_nouveau:sourceberry_bush',2,'bc28fb','spellbloom_tonic',action='mana')
fruit('skyberry','aether:blue_berry',2,'189cff','cloudbreak_cordial',[effect('slow_falling')])
fruit('grimberry','the_deep_void:grimberry',2,'147d79','grimberry_rush',clone='the_deep_void:grimberry_juice',agent='minecraft:wither_rose')
fruit('lemon','fruitsdelight:lemon',1,'f5f318','bittergold_tonic',[effect('haste',200)],'lemon')
fruit('pineapple','fruitsdelight:pineapple_slice',2,'ffc922','goldrush_fizz',[effect('haste')])
fruit('melon','minecraft:melon_slice',2,'ff5187','summerheart_cordial',clone='farmersdelight:melon_juice')
fruit('hamimelon','fruitsdelight:hamimelon_slice',2,'ff9b4d','dawnheart_cordial',[effect('absorption')])
fruit('chorus','minecraft:chorus_fruit',1,'e663fa','voidstep_elixir',[effect('slow_falling')],'blink',agent='minecraft:chorus_flower')
# Additional native berries: edible enchanted berry remains separate from ordinary Aether fruit.
fruit('enchanted_skyberry','aether:enchanted_berry',2,'75e3ff','enchanted_cloudbreak',[effect('slow_falling'),effect('regeneration',200)])
fruit('torchberry','twilightforest:torchberries',2,'ff8d26','torchlight_tonic',[effect('night_vision')])
fruit('belladonna','hexerei:belladonna_berries',2,'552366','nightshade_veil',[effect('invisibility'),effect('poison',100,0,.25)],agent='minecraft:fermented_spider_eye')
rows[-2]['unsafe']=True
profiles[NS+'belladonna_juice']['effects']=[effect('poison',200)]
# Coffee cherries and rose hips are fruit, not their processed beans/leaves.
fruit('coffee_cherry','farmersrespite:coffee_berries',2,'bc2936','cascara_tonic',[effect('farmersrespite:caffeinated',1200)])
fruit('rose_hip','farmersrespite:rose_hips',2,'e24731','rose_hip_tea',native='farmersrespite:rose_hip_tea')
# Additional pinned inventory stock, verified in native registrations (not translation guesses).
fruit('white_apple','aether:white_apple',1,'eeefce','cloudwhite_remedy',[effect('aether:remedy',300)])
fruit('firethorn','goety:firethorn_berries',2,'ec6325','firethorn_guard',[effect('fire_resistance')])
fruit('coconut','natures_spirit:coconut_half',2,'e7dfc7','palmshelter_cordial',[effect('water_breathing')])
fruit('olive','natures_spirit:olives',2,'778331','groveguard_tonic',[effect('resistance')])
# Olives are lipid-bearing fruit, not a new sugar source.
rows[-2]['nutrition']={'fats':.025}; rows[-1]['nutrition']={'fats':.025}
add('coconut_milk','natures_spirit:coconut_half',2,'eee8d7',group='fats',process='milk')

# Vegetable and grain bases; no sugar-only nutrition invention for leaves/herbs.
for plant,ingredient,count,colour,brew,fx,action,native,process in [
 ('carrot','minecraft:carrot',1,'f98a16','goldeneye_tonic',[effect('night_vision')],'none',None,'juice'),
 ('desert_turnip','natures_spirit:desert_turnip',1,'e4cbb3','duneward_kvass',[effect('farmersdelight:comfort')],'none',None,'juice'),
 ('corn','fallout_wastelands_:corn',1,'e8c54d','sungrain_kvass',[effect('speed')],'none',None,'milk'),
 ('beetroot','minecraft:beetroot',1,'b3275a','strongroot_ale',[],'none','brewinandchewin:strongroot_ale','juice'),
 ('tomato','farmersdelight:tomato',1,'ef3b2e','bloody_mary',[],'none','brewinandchewin:bloody_mary','juice'),
 ('cabbage','farmersdelight:cabbage_leaf',2,'73bc51','greenward_kvass',[effect('farmersdelight:comfort')],'none',None,'juice'),
 ('onion','farmersdelight:onion',1,'dfd4a2','hearthroot_infusion',[],'cure_harmful_one',None,'juice'),
 ('garlic','ubesdelight:garlic',1,'e9e4cd','whiteclove_tonic',[],'poison',None,'juice'),
 ('potato','minecraft:potato',1,'c6ae75','vodka',[],'none','brewinandchewin:vodka','juice'),
 ('wheat','minecraft:wheat',1,'e9cf9e','beer',[],'none','brewinandchewin:beer','milk'),
 ('rice','farmersdelight:rice',1,'ece8db','rice_wine',[],'none','brewinandchewin:rice_wine','milk'),
 ('pumpkin','farmersdelight:pumpkin_slice',2,'f6a51c','harvestgold_brew',[effect('absorption')],'none',None,'juice'),
 ('cane','minecraft:sugar_cane',1,'8fc943','canefire_brew',[effect('speed')],'none',None,'juice'),
 ('kelp','minecraft:kelp',2,'376e30','tideward_infusion',[effect('water_breathing')],'none',None,'juice'),
 ('ginger','ubesdelight:ginger',1,'d7a45b','hearthfire_ginger_tea',[],'nausea',None,'juice'),
 ('ube','ubesdelight:ube',1,'9c67bb','ube_milk_tea',[],'none','ubesdelight:milk_tea_ube','milk')]:
    group='grains' if plant in ['wheat','rice','corn'] else 'vegetables'
    plain=add(plant+('_milk' if process=='milk' else '_juice'),ingredient,count,colour,group=group,process=process)
    # Potato vodka uses the existing processed potato/mash route, not a raw juice substitution.
    if plant in ['potato','ube']:
        coverage.append(dict(plant=plant,ingredient=ingredient,portion=count,plain=NS+plain,brew=native,
            route='native processed-starch vodka / actual milk-and-tea preparation retained'))
        continue
    if plant in ['onion','garlic','ginger','kelp']:
        add(brew,ingredient,count,colour,group=None,effects=fx,action=action,process='infusion',food=0)
    else:
        add(brew,colour=colour,group=group,effects=fx,action=action,base=plain,agent='#better_drinking_water:safe_starter_flowers',native=native,process='primary')
    coverage.append(dict(plant=plant,ingredient=ingredient,portion=count,plain=NS+plain,brew=native or NS+brew))
# Pumpkin seed milk is a distinct processed seed preparation, not raw pumpkin juice.
add('pumpkin_seed_milk','minecraft:pumpkin_seeds',2,'e7d9b0',group='fats',process='milk')

for name,ingredient,colour,fx,action in [
 ('lemongrass_infusion','ubesdelight:lemongrass','b3ce51',[effect('farmersdelight:comfort')],'none'),
 ('redpetal_tea','minecraft:poppy','d53646',[effect('farmersdelight:comfort')],'none'),
 ('marshlight_infusion','minecraft:blue_orchid','40c2ef',[effect('water_breathing')],'none'),
 ('violetguard_tea','minecraft:allium','bf67ce',[],'poison'),
 ('clearbrook_infusion','minecraft:azure_bluet','d8eaf3',[effect('night_vision',600)],'none'),
 ('emberpetal_tea','minecraft:red_tulip','e3342c',[effect('strength',600)],'none'),
 ('copperpetal_tea','minecraft:orange_tulip','e68427',[effect('haste',600)],'none'),
 ('ivorypetal_tea','minecraft:white_tulip','ece8ca',[effect('resistance',600)],'none'),
 ('blushpetal_tea','minecraft:pink_tulip','ef9ab9',[effect('regeneration',200)],'none'),
 ('meadowrest_tea','minecraft:oxeye_daisy','f0d258',[effect('regeneration',400)],'none'),
 ('bluewind_infusion','minecraft:cornflower','4b7aea',[effect('speed')],'none'),
 ('sunward_tea','minecraft:sunflower','f8c520',[effect('fire_resistance',600)],'none'),
 ('evening_lilac_tea','minecraft:lilac','ba79bc',[effect('farmersdelight:comfort')],'none'),
 ('peony_heartcordial','minecraft:peony','ec79b6',[effect('absorption')],'none'),
 ('stillwater_infusion','hexerei:flowering_lily_pad','85ba9a',[effect('water_breathing')],'none'),
 ('dockside_bitter','hexerei:yellow_dock_leaves','aab34a',[],'cure_harmful_one'),
 ('dreamwalker_tea','hexerei:mugwort_leaves','6d8b57',[effect('night_vision'),effect('nausea',100)],'none'),
 ('mandrake_fortifier','hexerei:mandrake_root','a68c56',[effect('resistance'),effect('slowness',200)],'none'),
 ('widows_whisper','minecraft:lily_of_the_valley','b8d1c1',[effect('invisibility'),effect('weakness',200)],'none'),
 ('gravebloom_draught','minecraft:wither_rose','57283d',[effect('strength'),effect('wither',100)],'none'),
 ('warped_veil','minecraft:warped_fungus','1aa8a0',[effect('water_breathing'),effect('nausea',100)],'none')]:
    add(name,ingredient,1,colour,group=None,effects=fx,action=action,process='infusion',food=0)
    coverage.append(dict(plant=ingredient,plain=None,brew=NS+name,route='heated kettle infusion'))
coverage.append(dict(plant='farmersrespite:wither_roots',plain=None,brew=None,route='Unavailable inventory stock: FRItems registers no Wither Roots item; WitherRootsBlock is an unharvestable environmental hazard, not a Purulent Tea ingredient. No fabricated item or harvest mechanic.'))
add('summerheart_reserve',colour='ed457a',action='heal4',base='summerheart_cordial',agent='minecraft:nether_wart',process='secondary')
add('blackwake_reserve',colour='125c63',effects=[effect('haste',1200,1),effect('weakness',200)],base='grimberry_rush',agent='minecraft:crimson_fungus',process='secondary')
# Retained native families and mixtures are explicit coverage, not silently culled by normalization.
retained=['farmersrespite:green_tea','farmersrespite:yellow_tea','farmersrespite:black_tea','create:builders_tea',
 'farmersrespite:coffee','farmersdelight:hot_cocoa','farmersrespite:dandelion_tea','farmersrespite:purulent_tea',
 'farmersrespite:gamblers_tea','malum:cursed_sap','brewinandchewin:kombucha','ubesdelight:milk_tea_ube','fruitsdelight:mango_milkshake',
 'fruitsdelight:bellini_cocktail','brewinandchewin:steel_toe_stout','brewinandchewin:pale_jane','brewinandchewin:red_rum',
 'brewinandchewin:salty_folly','brewinandchewin:saccharine_rum']

by_name={r['name']:r for r in rows}
assert len(by_name)==len(rows), 'Duplicate authored plant drink'
for r in rows:
    if r['base']:
        r['mods']=sorted(set(r['mods']) | set(by_name[r['base']]['mods']))
    p=profiles[r['item']]
    r['role']='unsafe' if r['unsafe'] else ('buff' if p['effects'] or p['action']!='none' else 'hydration_plus')
    assert 1 <= r['count'] <= 4
out={}
def emit(path,obj): out[path]=(json.dumps(obj,indent=2,ensure_ascii=False)+'\n').encode()
def recipe(name,obj,mods=()):
    if mods: obj['conditions']=[dict(type='forge:mod_loaded',modid=m) for m in sorted(set(mods))]
    emit(RES/'data/better_drinking_water/recipes/plant'/f'{name}.json',obj)
def ing(s): return {'tag':s[1:]} if s.startswith('#') else {'item':s}
for r in rows:
    name,item=r['name'],r['item']; fluid=NS+name; mods=r['mods']
    vessel=profiles[item]['remainder']
    if r['ingredient']:
        ingredients=[ing(r['ingredient']) for _ in range(r['count'])]
        if r['process']=='juice':
            recipe(name+'_hand',dict(type='minecraft:crafting_shapeless',ingredients=ingredients+[ing('minecraft:glass_bottle')],result=dict(item=item)),mods)
            recipe(name+'_press',dict(type='create:compacting',ingredients=ingredients,results=[dict(fluid=fluid,amount=250)]),mods+['create'])
        else:
            # Grain/seed/ube milks and botanicals use water + heated kettle, not fictitious raw juices.
            recipe(name+'_steep',dict(type='farmersrespite:brewing',base=dict(fluid='minecraft:water',count=250),ingredients=ingredients,
                result=dict(fluid=fluid,count=250),cookingtime=1200,experience=.2),mods+['farmersrespite'])
    if r['base']:
        recipe(name+'_brew',dict(type='brewinandchewin:fermenting',basefluid=dict(fluid=NS+r['base'],count=1000),ingredients=[ing(r['agent'])],
                result=dict(fluid=fluid,count=1000),fermentingtime=4800 if r['process']=='secondary' else 9600,temperature=3,experience=.5),mods+['brewinandchewin'])
    recipe(name+'_fill',dict(type='create:filling',ingredients=[ing(vessel),dict(fluid=fluid,amount=250)],results=[dict(item=item)]),mods+['create'])
    recipe(name+'_empty',dict(type='create:emptying',ingredients=[ing(item)],results=[dict(item=vessel),dict(fluid=fluid,amount=250)]),mods+['create'])
    recipe(name+'_pour',dict(type='brewinandchewin:keg_pouring',fluid=fluid,amount=250,filling=True,strict=False,output=dict(item=item)),mods+['brewinandchewin'])
    recipe(name+'_kettle_pour',dict(type='farmersrespite:kettle_pouring',fluid=fluid,amount=250,container=ing(vessel),output=dict(item=item)),mods+['farmersrespite'])
    if item.startswith(NS): emit(RES/'assets/better_drinking_water/models/item'/f'{name}.json',dict(parent='minecraft:item/generated',textures=dict(layer0=NS+'item/'+name)))
emit(RES/'plant_drinks.json',dict(version=1,serving_mb=250,drinks=rows,coverage=coverage,retained=retained))
emit(RES/'data/better_drinking_water/tags/items/safe_starter_flowers.json',dict(replace=False,values=['minecraft:'+s for s in ['dandelion','poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip','oxeye_daisy','cornflower','sunflower','lilac','rose_bush','peony']]))
# Finite explicit category matrix. No discovery via item-name keywords at runtime.
water = {'create:builders_tea'} | {'farmersrespite:'+prefix+name for prefix in ['', 'long_', 'strong_']
    for name in ['green_tea','yellow_tea','black_tea','rose_hip_tea','dandelion_tea','gamblers_tea','coffee']}
fermented = {'brewinandchewin:'+name for name in ['beer','rice_wine','mead','kombucha','strongroot_ale','steel_toe_stout']}
spirits = {'brewinandchewin:'+name for name in ['vodka','red_rum','saccharine_rum']}
mixed = {'brewinandchewin:'+name for name in ['bloody_mary','dread_nog','egg_grog','glittering_grenadine','pale_jane','salty_folly','withering_dross']} | {'fruitsdelight:bellini_cocktail'}
water |= {r['item'] for r in rows if r['process']=='infusion'}
quantities = {'WATER': (4,5), 'JUICE_MILK': (3,4), 'FERMENTED_ALCOHOL': (3,3), 'SPIRIT': (1,0), 'MIXED_ALCOHOL': (2,2)}
for item,p in profiles.items():
    category = 'WATER' if item in water else 'FERMENTED_ALCOHOL' if item in fermented else 'SPIRIT' if item in spirits else 'MIXED_ALCOHOL' if item in mixed else 'JUICE_MILK'
    volume = 1000 if item in ['minecraft:milk_bucket','aether:skyroot_milk_bucket'] else 250
    p['hydration_class'],p['serving_mb']=category,volume
    p['hydration'],p['quenching']=[x*(volume//250) for x in quantities[category]]
    p['source'] = p['source'].split('; hydration basis:')[0] + '; hydration basis: '+category+' per 250 mB'
emit(RES/'drinks.json',dict(version=1,drinks=sorted(profiles.values(),key=lambda r:r['id']),excluded=old['excluded']))
lang=json.loads((RES/'assets/better_drinking_water/lang/en_us.json').read_text())
lang={k:v for k,v in lang.items() if not k.startswith(('fluid_type.better_drinking_water.','fluid.better_drinking_water.'))
      and (not k.startswith('item.better_drinking_water.') or k=='item.better_drinking_water.rain_collector')}
lang.update({'tooltip.better_drinking_water.plant.hydration_plus':'250 mB · Hydration+ — nutrition without specialist effects.',
             'tooltip.better_drinking_water.plant.buff':'250 mB · Buff',
             'tooltip.better_drinking_water.action.poison':'removes Poison on completion',
             'tooltip.better_drinking_water.action.nausea':'removes Nausea on completion',
             'tooltip.better_drinking_water.action.orange':'cleanses Poison and Wither on completion',
             'tooltip.better_drinking_water.action.lemon':'cleanses Mining Fatigue on completion',
             'tooltip.better_drinking_water.action.blink':'random blink on completion',
             'tooltip.better_drinking_water.action.mana':'restores 20 mana per serving',
             'tooltip.better_drinking_water.action.heal2':'heals 2 HP per serving',
             'tooltip.better_drinking_water.action.heal4':'heals 4 HP per serving',
             'tooltip.better_drinking_water.action.cure_harmful_one':'cleanses one harmful, milk-curable effect on completion',
             'tooltip.better_drinking_water.plant.unsafe':'250 mB · Unsafe — contains Poison.',
             'tooltip.better_drinking_water.completion':'250 mB · Buff — special action releases only after finishing the serving.'})
for r in rows:
    label=r['name'].replace('_',' ').title()
    if r['item'].startswith(NS): lang['item.better_drinking_water.'+r['name']]=label
    lang['fluid_type.better_drinking_water.'+r['name']]=label
    lang['fluid.better_drinking_water.'+r['name']]=label
emit(RES/'assets/better_drinking_water/lang/en_us.json',lang)
# Donor bytes are authored under assets-source, with MIT notice and pinned original hash.
donor=ROOT/'assets-source/apple_cider.png'
if donor.exists():
    out[RES/'META-INF/plant-drinks-donor.txt']=(
        'Plant-drink glass textures retinted from Farmer\'s Delight apple_cider.png and melon_juice.png by vectorwing.\n'
        'Source: https://github.com/vectorwing/FarmersDelight/tree/1.20\n'
        'Apple cider SHA-256: b5de2d077829026f5df10ee11341c7d4e0088658eb3daca297bb8b81c3acc6b7\n'
        'Melon juice SHA-256: ca1329709233c6c671893f4d638e045556f05e10d3e053477a67b951dbf9bdcb\n\n'
        +(ROOT/'assets-source/LICENSE-FarmersDelight.txt').read_text()).encode()
    assert hashlib.sha256(donor.read_bytes()).hexdigest()=='b5de2d077829026f5df10ee11341c7d4e0088658eb3daca297bb8b81c3acc6b7'
    melon=ROOT/'assets-source/melon_juice.png'
    assert hashlib.sha256(melon.read_bytes()).hexdigest()=='ca1329709233c6c671893f4d638e045556f05e10d3e053477a67b951dbf9bdcb'
    for r in rows:
        if not r['item'].startswith(NS): continue
        image=Image.open(melon if r['process'] in ['juice','milk'] else donor).convert('RGBA')
        tint=tuple(int(r['colour'][i:i+2],16)/255 for i in (0,2,4))
        hue,sat,_=colorsys.rgb_to_hsv(*tint)
        pixels=[]
        for index,(red,green,blue,alpha) in enumerate(image.get_flattened_data()):
            h,s,v=colorsys.rgb_to_hsv(red/255,green/255,blue/255)
            x,y=index%16,index//16
            # Geometric liquid mask excludes rim, handle, garnish, straw and all blue glass.
            if alpha and 4<=x<=10 and 7<=y<=13 and s>.35 and (h<.18 or h>.95):
                red,green,blue=(round(c*255) for c in colorsys.hsv_to_rgb(hue,min(1,sat*1.1),v))
            pixels.append((red,green,blue,alpha))
        retint=image.copy(); retint.putdata(pixels); buf=io.BytesIO();retint.save(buf,format='PNG')
        out[RES/'assets/better_drinking_water/textures/item'/f'{r["name"]}.png']=buf.getvalue()
parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
wrong=[]
for path,data in out.items():
    if args.check:
        if not path.exists() or path.read_bytes()!=data: wrong.append(str(path.relative_to(ROOT)))
    else:
        path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
for directory in [RES/'data/better_drinking_water/recipes/plant', RES/'assets/better_drinking_water/models/item', RES/'assets/better_drinking_water/textures/item']:
    for path in directory.glob('*'):
        if path.name.startswith('rain_collector'): continue
        if path.is_file() and path not in out: wrong.append(str(path.relative_to(ROOT)))
if wrong: raise SystemExit('Stale or obsolete generated plant resources (no deletion performed): '+', '.join(wrong))
print(f'{len(rows)} fluid profiles; {len(coverage)} plant entries; {len(out)} reproducible outputs')
