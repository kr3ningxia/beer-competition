<template>
  <footer :class="['site-filing', { 'site-filing--with-nav': withBottomNav }]" aria-label="网站备案信息">
    <div class="site-filing__inner">
      <span class="site-filing__owner">© 上海啤事文化传播有限公司</span>
      <nav class="site-filing__links" aria-label="备案查询">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">
          沪ICP备2024061956号-3
        </a>
        <span class="site-filing__dot" aria-hidden="true" />
        <a
          class="site-filing__police"
          href="https://beian.mps.gov.cn/#/query/webSearch?code=31011702892028"
          target="_blank"
          rel="noopener noreferrer"
        >
          <img :src="policeBadge" alt="" width="16" height="18">
          <span>沪公网安备31011702892028号</span>
        </a>
      </nav>
    </div>
  </footer>
</template>

<script setup>
defineProps({
  withBottomNav: {
    type: Boolean,
    default: false,
  },
})

const policeBadge = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACQAAAAoCAYAAACWwljjAAAFQklEQVRYw+3Wa1BUdRjH8SOpMeg4WhZGpDIxiaaTeUFgWrxE4AVRQJGlRRAVIV1JkbgMgQLi5AVBQSVLSp0xlEAUKBEEFZCrCstll8UV2AV2YbmoGCrYv31+R95UL5pmmtamZ+bz6rz5nvOc/5zDcX9jGLs/iTxuyvIlWYkRFeTHA2HVRFtzfhthTG5KuH96/vUgNlC4mMgyw1NJit/aAXLKazYje9xtIMZ/OZz50gW+9hcNkvoLEemEPbnrSP47QYwxQ5Ifv54RqzcXwFFvSyjaOhfavN8F7Y5ZcC/HH9JOB4LNa9Zw5YA76OZV8vIGMdZtSp7cDrtOnOavYiQhTAiPwi1AMtIQaqyngsxpBtw2GAGDKfaQmpUAa6xc4Vfp4UtEdzAMycsT9JQ1Tyctl/2eEkuTlYysF/rCUNxMqDEzgTqzSXBnpgnIHCzgjvEEuD52DLBr3rA1MAaWmNtB582wdtIljZ9G9D+IPU6aTxIPBjHCcXvg3CEh9K2fDLWvjIH6D6fwTIyheuwEqLUyhzLOALq8pkN+bgRw3HY4FBsMzxojZxP9DequLjAlQwVrbpIjhyIY4UYGQ/buhdBqPxlk3Gion2IMDQIz3kJe/ZS34I7uHkmD7VSQVgYDNyIAwsNCgfXGXoOBPjP9DKrOCAogA2etGTmTHAMcFwFZye7wS5QlVHGjoEw4A2qPCUBZ6AzNcQ5Q/YYRdO+YB1U3dsDwypLio4FJ3ECryIzWz6Cm3NgTRHN8HiPF6eHAGSbAdh8feFZkB7krzaHE9h2o85sDsiAbkIsXQMN+e2CtGyF0kzdwXCgU5++D/ouLQFV4OEU/g2Q/iNuIPNaKkQflAWBqexxGjhLDVUcL6IwSQN3SGVChe6FJg9dckCx6D1QBliDZLIAxo7eA8eyv4KE0BJqTrHkZvnL9DJKn+Twmt0NsGGHZy2Dn3kQYfsQ53Hh4/r4RNGz8AIpdzKEuaAF0RC2E57MmQgE3ATjuM/CPiANW7AqSfQJQ5vk362eQKmd3JrmXsoSRocpNIMnbB9zbceDIWUPmuHFQNMkISqa9DpUvNK6YDpW2s8DfwBK48WFQnhMCgzUBoLy0BrRVe5P0NWjPLdKUsJiR1tR1wGp8IeZwMgx/SrgRvjxuAziNcwLvyathLOcJHLflhRDYGRYFrNET2rJ5yvPLoas0tOj/oL8UpC4JHyTSU+6MNCS4gvKoAB5WiKG+MAQSg0WwLXQ/ZJ3xhao0FxB5hYCbUwAEfhEF3Td8QP2dAOQnPwFlxgrolUVq9TPoaX+ZB2nLc2Gk6awj1MU78HZZwJMid2Byb550JQwVO0NfxlJgdz14vWKeRAiK6DlQF28PLZdcoLNcBIO92bb6GTQ8Q/13RURT6tlH2gvXMlITLYD6uI+gp2ozdF0VQXumM6ivCqGvahM8kPiDItkeGo8tB025GFQ3xFrSr06zI3/4yde7oN7m0sWk5eKWDqK5JWJQvAHac9ygq3Adr9gTNNc3QG85rzPfHe5/7wDtPwuhp/Zz6CjyhaZzwi6ivfetHdH/oP77+3PJQOsuRnqkQdCa4wWqyx6gyecpL64GTaEX7ycXUJz4GJp1B4O0X/Hg0Xp1tFV+8Ei1k6c5coHofxBrrzQinbKYo0SVJ+wn6iurGHlY5gY911aDJnMFaHXXiDp9GQyvtKfUA9QFTtBZ7gPdit0tpFd9OpwwFmlA9D/o9yNLDpxIKmI8PMnNSNtviCLVpYTITzrXEGWaq4qos0WgOPdpCenIF+eRrurjB4k0PXopYZG6gMg/D/gNBUxhAbSAmKMAAAAASUVORK5CYII='
</script>

<style scoped>
.site-filing {
  position: relative;
  z-index: 10;
  width: 100%;
  padding: 16px 16px calc(18px + env(safe-area-inset-bottom));
  color: #667085;
  border-top: 1px solid rgba(24, 34, 47, 0.1);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(238, 241, 240, 0.94)),
    #eef1f0;
  font-size: 11px;
  line-height: 1.45;
}

.site-filing--with-nav {
  padding-bottom: calc(78px + env(safe-area-inset-bottom));
}

.site-filing__inner {
  display: grid;
  justify-items: center;
  gap: 7px;
  width: min(100%, 520px);
  margin: 0 auto;
  text-align: center;
}

.site-filing__owner {
  color: #475467;
  font-weight: 750;
}

.site-filing__links,
.site-filing__police {
  display: flex;
  align-items: center;
}

.site-filing__links {
  flex-wrap: wrap;
  justify-content: center;
  gap: 7px;
}

.site-filing a {
  color: inherit;
  text-decoration: none;
  transition: color 0.16s ease;
}

.site-filing a:hover,
.site-filing a:focus-visible {
  color: #a75517;
}

.site-filing__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.48;
}

.site-filing__police {
  gap: 5px;
}

.site-filing__police img {
  flex: 0 0 auto;
  width: 16px;
  height: 18px;
  object-fit: contain;
}

@media (max-width: 380px) {
  .site-filing__links {
    display: grid;
    justify-items: center;
    gap: 5px;
  }

  .site-filing__dot {
    display: none;
  }
}
</style>
