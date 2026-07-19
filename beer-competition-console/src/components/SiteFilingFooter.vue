<template>
  <footer :class="['site-filing', `site-filing--${tone}`, { 'site-filing--compact': compact }]" aria-label="网站备案信息">
    <div class="site-filing__inner">
      <span class="site-filing__owner">© 上海啤事文化传播有限公司</span>
      <span class="site-filing__divider" aria-hidden="true" />
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
          <img :src="policeBadge" alt="" width="18" height="20">
          <span>沪公网安备31011702892028号</span>
        </a>
      </nav>
    </div>
  </footer>
</template>

<script setup>
defineProps({
  tone: {
    type: String,
    default: 'warm',
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const policeBadge = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACQAAAAoCAYAAACWwljjAAAFQklEQVRYw+3Wa1BUdRjH8SOpMeg4WhZGpDIxiaaTeUFgWrxE4AVRQJGlRRAVIV1JkbgMgQLi5AVBQSVLSp0xlEAUKBEEFZCrCstll8UV2AV2YbmoGCrYv31+R95UL5pmmtamZ+bz6rz5nvOc/5zDcX9jGLs/iTxuyvIlWYkRFeTHA2HVRFtzfhthTG5KuH96/vUgNlC4mMgyw1NJit/aAXLKazYje9xtIMZ/OZz50gW+9hcNkvoLEemEPbnrSP47QYwxQ5Ifv54RqzcXwFFvSyjaOhfavN8F7Y5ZcC/HH9JOB4LNa9Zw5YA76OZV8vIGMdZtSp7cDrtOnOavYiQhTAiPwi1AMtIQaqyngsxpBtw2GAGDKfaQmpUAa6xc4Vfp4UtEdzAMycsT9JQ1Tyctl/2eEkuTlYysF/rCUNxMqDEzgTqzSXBnpgnIHCzgjvEEuD52DLBr3rA1MAaWmNtB582wdtIljZ9G9D+IPU6aTxIPBjHCcXvg3CEh9K2fDLWvjIH6D6fwTIyheuwEqLUyhzLOALq8pkN+bgRw3HY4FBsMzxojZxP9DequLjAlQwVrbpIjhyIY4UYGQ/buhdBqPxlk3Gion2IMDQIz3kJe/ZS34I7uHkmD7VSQVgYDNyIAwsNCgfXGXoOBPjP9DKrOCAogA2etGTmTHAMcFwFZye7wS5QlVHGjoEw4A2qPCUBZ6AzNcQ5Q/YYRdO+YB1U3dsDwypLio4FJ3ECryIzWz6Cm3NgTRHN8HiPF6eHAGSbAdh8feFZkB7krzaHE9h2o85sDsiAbkIsXQMN+e2CtGyF0kzdwXCgU5++D/ouLQFV4OEU/g2Q/iNuIPNaKkQflAWBqexxGjhLDVUcL6IwSQN3SGVChe6FJg9dckCx6D1QBliDZLIAxo7eA8eyv4KE0BJqTrHkZvnL9DJKn+Twmt0NsGGHZy2Dn3kQYfsQ53Hh4/r4RNGz8AIpdzKEuaAF0RC2E57MmQgE3ATjuM/CPiANW7AqSfQJQ5vk362eQKmd3JrmXsoSRocpNIMnbB9zbceDIWUPmuHFQNMkISqa9DpUvNK6YDpW2s8DfwBK48WFQnhMCgzUBoLy0BrRVe5P0NWjPLdKUsJiR1tR1wGp8IeZwMgx/SrgRvjxuAziNcwLvyathLOcJHLflhRDYGRYFrNET2rJ5yvPLoas0tOj/oL8UpC4JHyTSU+6MNCS4gvKoAB5WiKG+MAQSg0WwLXQ/ZJ3xhao0FxB5hYCbUwAEfhEF3Td8QP2dAOQnPwFlxgrolUVq9TPoaX+ZB2nLc2Gk6awj1MU78HZZwJMid2Byb550JQwVO0NfxlJgdz14vWKeRAiK6DlQF28PLZdcoLNcBIO92bb6GTQ8Q/13RURT6tlH2gvXMlITLYD6uI+gp2ozdF0VQXumM6ivCqGvahM8kPiDItkeGo8tB025GFQ3xFrSr06zI3/4yde7oN7m0sWk5eKWDqK5JWJQvAHac9ygq3Adr9gTNNc3QG85rzPfHe5/7wDtPwuhp/Zz6CjyhaZzwi6ivfetHdH/oP77+3PJQOsuRnqkQdCa4wWqyx6gyecpL64GTaEX7ycXUJz4GJp1B4O0X/Hg0Xp1tFV+8Ei1k6c5coHofxBrrzQinbKYo0SVJ+wn6iurGHlY5gY911aDJnMFaHXXiDp9GQyvtKfUA9QFTtBZ7gPdit0tpFd9OpwwFmlA9D/o9yNLDpxIKmI8PMnNSNtviCLVpYTITzrXEGWaq4qos0WgOPdpCenIF+eRrurjB4k0PXopYZG6gMg/D/gNBUxhAbSAmKMAAAAASUVORK5CYII='
</script>

<style scoped>
.site-filing {
  position: relative;
  z-index: 2;
  width: 100%;
  padding: 18px 24px 20px;
  color: #776c60;
  border-top: 1px solid rgba(87, 58, 26, 0.1);
  background: rgba(255, 250, 240, 0.48);
  font-size: 12px;
  line-height: 1.5;
}

.site-filing__inner,
.site-filing__links,
.site-filing__police {
  display: flex;
  align-items: center;
}

.site-filing__inner {
  justify-content: center;
  gap: 12px;
  max-width: 1280px;
  margin: 0 auto;
  text-align: center;
}

.site-filing__owner {
  color: #5f5549;
  font-weight: 700;
}

.site-filing__divider {
  width: 1px;
  height: 14px;
  background: rgba(87, 58, 26, 0.18);
}

.site-filing__links {
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.site-filing a {
  color: inherit;
  text-decoration: none;
  transition: color 0.16s ease;
}

.site-filing a:hover,
.site-filing a:focus-visible {
  color: #9a6113;
}

.site-filing__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.46;
}

.site-filing__police {
  gap: 6px;
}

.site-filing__police img {
  flex: 0 0 auto;
  width: 18px;
  height: 20px;
  object-fit: contain;
}

.site-filing--dark {
  color: #73878f;
  border-top-color: rgba(218, 231, 236, 0.08);
  background: rgba(7, 12, 15, 0.26);
}

.site-filing--dark .site-filing__owner {
  color: #a9bbc1;
}

.site-filing--dark .site-filing__divider {
  background: rgba(218, 231, 236, 0.13);
}

.site-filing--dark a:hover,
.site-filing--dark a:focus-visible {
  color: #e7bd4d;
}

.site-filing--compact {
  margin-top: 12px;
  padding: 12px 4px 0;
  background: transparent;
  font-size: 10px;
}

.site-filing--compact .site-filing__inner,
.site-filing--compact .site-filing__links {
  display: grid;
  justify-items: start;
  gap: 5px;
  text-align: left;
}

.site-filing--compact .site-filing__divider,
.site-filing--compact .site-filing__dot {
  display: none;
}

.site-filing--compact .site-filing__police {
  gap: 5px;
}

.site-filing--compact .site-filing__police img {
  width: 14px;
  height: 16px;
}

@media (max-width: 720px) {
  .site-filing {
    padding: 16px 14px 18px;
  }

  .site-filing__inner {
    display: grid;
    justify-items: center;
    gap: 7px;
  }

  .site-filing__divider {
    display: none;
  }

  .site-filing__links {
    row-gap: 6px;
  }
}
</style>
