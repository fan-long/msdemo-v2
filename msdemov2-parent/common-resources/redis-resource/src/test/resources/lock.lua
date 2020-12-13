--redis-cli -c --eval lock.lua unit1 , resource locker mode value
local r= redis.call('hsetnx',KEYS[1],ARGV[1],ARGV[2])
if r==0 then --锁已存在
  local locker=redis.call('hget',KEYS[1],ARGV[1])
  local mode=redis.call('hget',KEYS[1],ARGV[1]..'_M')
  if ARGV[3]=='S' then --申请共享锁
    if (mode~='X') then --当前为共享锁  
      if locker~=ARGV[2] then --非原始拥有者，计数器增加
        redis.call('hincrby',KEYS[1],ARGV[1]..'_M',1)
      end
      return ARGV[2] --加锁成功
    else --当前为排它锁
       return '';   --锁冲突，加锁失败       
    end
  else --申请排它锁
    if (mode=='X') then --当前为排它锁
      return locker    --返回原始拥有者，调用方判断是否成功
    else --当前为共享锁
      return '';   --锁冲突，加锁失败
    end  
  end  
else  
  if (ARGV[3]=='X') then --申请排它锁，锁类型为'X'
    redis.call('hmset',KEYS[1], ARGV[1]..'_M', ARGV[3], ARGV[1]..'_V', ARGV[4])
  else --申请共享锁，锁类型为计数器
    redis.call('hmset',KEYS[1], ARGV[1]..'_M', 1, ARGV[1]..'_V', ARGV[4])
  end   
  return ARGV[2] --加锁成功
end

