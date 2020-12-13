-- redis-cli --eval timing.lua 0
-- zrangebyscore d 0 10 limit 0 1

-- 获取当前时间戳
--local timestamp = redis.call("TIME")
--local now = math.floor((timestamp[1] * 1000000 + timestamp[2]) / 1000)

local r=redis.call('zrangebyscore',KEYS[1],0,ARGV[1], 'limit',0,1)
if r[1]~=nil then
  redis.call('sadd','{'..KEYS[1]..'}'..ARGV[1],r[1])
  redis.call('zrem',KEYS[1],r[1])
end
return r