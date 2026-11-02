import json, random
from pathlib import Path
from PIL import Image, ImageDraw
root=Path('/home/keelfy/github/restless')
assets=root/'apps/mod/core/src/main/resources/assets/restless'
# Integer texels only; every model unit spans four texels on every face.
top=Image.open(root/'output/imagegen/ouija-source.png').convert('RGB').resize((64,64),Image.Resampling.NEAREST).quantize(colors=16).convert('RGBA')
top.save(assets/'textures/block/ouija_board.png')
rng=random.Random(733)
wood=Image.new('RGBA',(64,64),(91,55,30,255))
d=ImageDraw.Draw(wood)
for y in range(64):
 for x in range(0,64,4):
  color=rng.choice(['#52321e','#5b371e','#644024','#6d4528'])
  d.line((x,y,min(x+rng.randrange(3,10),63),y),fill=color)
wood.save(assets/'textures/block/ouija_board_wood.png')
# A flat inventory sprite with a transparent margin, independent of block geometry.
icon=Image.new('RGBA',(32,32))
icon.paste(top.resize((28,26),Image.Resampling.NEAREST),(2,3))
for x,y in [(2,3),(29,3),(2,28),(29,28)]: icon.putpixel((x,y),(0,0,0,0))
icon.save(assets/'textures/item/ouija_board.png')
def face(texture,uv): return {'uv':uv,'texture':texture}
elements=[]
for x0,z0,x1,z1 in [(0.5,0,15.5,16),(0,0.5,0.5,15.5),(15.5,0.5,16,15.5)]:
 elements.append({'from':[x0,0,z0],'to':[x1,1,z1],'faces':{
 'up':face('#top',[x0,z0,x1,z1]),
 'down':face('#wood',[x0,z0,x1,z1]),
 'north':face('#wood',[x0,0,x1,1]),'south':face('#wood',[x0,1,x1,2]),
 'west':face('#wood',[z0,2,z1,3]),'east':face('#wood',[z0,3,z1,4])}})
model={'parent':'minecraft:block/block','textures':{'particle':'restless:block/ouija_board_wood','top':'restless:block/ouija_board','wood':'restless:block/ouija_board_wood'},'elements':elements}
for path,obj in [
 ('models/block/ouija_board.json',model),
 ('models/item/ouija_board.json',{'parent':'minecraft:item/generated','textures':{'layer0':'restless:item/ouija_board'}}),
 ('items/ouija_board.json',{'model':{'type':'minecraft:model','model':'restless:item/ouija_board'}})]:
 (assets/path).write_text(json.dumps(obj,indent=2)+'\n')
