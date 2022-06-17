# 서비스 진단하기

## 2단계 - 성능 테스트

### 요구사항
- [x] 웹 성능 테스트
  - [x] 웹 성능 예산을 작성
  - [x] [WebPageTest](https://www.webpagetest.org/), [PageSpeed](https://pagespeed.web.dev/) 등 테스트해보고 개선이 필요한 부분을 파악

#### 1. 웹 성능예산은 어느정도가 적당하다고 생각하시나요
- 예비 분석
  - 사용자 트래픽이 많은 페이지 혹은 제품 방문 페이지 등 가장 중요한 페이지가 무엇인지 파악
  - PageSpeed를 활용하여 Desktop, Mobile 사이트 등에서 측정된 FCP, TTI 등의 지표를 확인

  > 메인, 회원가입, 로그인, 즐겨찾기, 경로검색

- 경쟁사 분석
  - 경쟁 사이트 또는 유사한 사이트의 성능을 조사
  - 연구에 따르면, 사용자는 응답시간이 20% 이상일 때 차이를 인식. 경쟁사 대비 20% 이상 성능 차이가 나는지 확인

  > - WebPageTest
  >
  > 구분 | subway |  naver
  > -----|-------:|-------:
  > Start Render | 2.730s | 1.000s
  > First Contentful Paint | 2.730s | 0.927s
  > Spped Index | 2.802s | 2.581s
  > Largest Contentful Paint | 2.780s | 3.398s
  > Comulative Layout Shift |  0.001 | 0.046
  > Total Blocking Time | 0.000s | 0.012s
  > Total Bytes | 2,493KB | 794KB
  > 
  > - PageSpeed
  > 
  > 구분 | subway |  naver 
  > -----|-------:|-------:
  > 성능 | 31 | 55
  > First Contentful Paint | 14.6s | 2.2s
  > Time to Interactive | 15.2s | 6.6s
  > Speed Index | 14.6s | 7.3s
  > Total Blocking Time | 560ms | 330ms
  > Largest Contentful Paint | 15.2s | 7.4s
  > Cumulative Layout Shift | 0.041 | 0.03

- 성능 기준 설정
  - 정량 / 시간 / 규칙 기반으로 산정
    - 메인 페이지의 모든 오브젝트 파일 크기는 10MB 미만으로 제한한다.
    - 모든 웹 페이지의 각 페이지 내 포함된 자바스크립트 크기는 1MB를 넘지 않아야 한다.
    - 검색 페이지에는 2MB 미만의 이미지가 포함되어야합니다.
    - LTE 환경에서의 모바일 기기의 Time to Interactive는 5초 미만이어야 한다.
    - Dom Content Loaded는 10초, First Meaningful Paint는 15초 미만이어야 한다.
    - Lighthouse 성능 감사에서 80 점 이상이어야한다.
  - 페이지로드 3초 미만
  - TTI 5초 미만
  - 압축된 리소스 최대 크기 200KB 미만

- 우선순위
  - 사용자에게 컨텐츠가 빠르게 노출되고 렌더링하는 것이 중요할 경우 FCP를 낮게 유지
  - 사용자가 관련 링크를 빠르게 클릭해야 할 경우 TTI의 우선

#### 2. 웹 성능예산을 바탕으로 현재 지하철 노선도 서비스는 어떤 부분을 개선하면 좋을까요
- 텍스트 압축 사용
  <details>
  <summary>접기/펼치기</summary>

  ```
  # nginx.conf
  events {}
  
  http {
    gzip on;
    gzip_disable "msie6";
    
        gzip_vary on;
        gzip_proxied any;
        gzip_comp_level 6;
        gzip_buffers 16 8k;
        gzip_http_version 1.1;
        gzip_min_length 256;
        gzip_types
            text/css
            text/javascript
            text/xml
            text/plain
            text/x-component
            application/javascript
            application/json
            application/xml
            application/rss+xml
            font/truetype
            font/opentype
            application/vnd.ms-fontobject
            image/svg+xml;
    
    upstream app {
    ...
  }
  
  # Dockerfile
  FROM nginx

  COPY nginx.conf /etc/nginx/nginx.conf
  COPY fullchain.pem /etc/letsencrypt/live/give928.kro.kr/fullchain.pem
  COPY privkey.pem /etc/letsencrypt/live/give928.kro.kr/privkey.pem
  ```
  ```shell
  $ docker stop proxy && docker rm proxy
  $ docker build -t reverse-proxy:0.0.3 .
  $ docker run -d -p 80:80 -p 443:443 --name proxy -v /var/log/nginx:/var/log/nginx reverse-proxy:0.0.3
  ```
  </details>

- 렌더링 차단 리소스 제거하기
  <details>
  <summary>접기/펼치기</summary>

  ```html
  <link href="//fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900" rel="preload" as="style" onload="this.rel='stylesheet'" />
  <noscript><link rel="stylesheet" href="//fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900"></noscript>
  <link href="//cdn.jsdelivr.net/npm/@mdi/font@5.0.45/css/materialdesignicons.min.css" rel="preload" as="style" onload="this.rel='stylesheet'" />
  <noscript><link rel="stylesheet" href="//cdn.jsdelivr.net/npm/@mdi/font@5.0.45/css/materialdesignicons.min.css"></noscript>
  <script src="/js/vendors.js" defer></script>
  <script src="/js/main.js" async></script>
  ```
  </details>

- 프론트엔드 리소스 지연 로딩 적용
  <details>
  <summary>접기/펼치기</summary>

  ```html
  // 기존
  import StationPage from '@/views/station/StationPage'
  // 변경
  const StationPage = () => import(/* webpackChunkName: "js/station/station" */'@/views/station/StationPage')
  ```
  </details>

- 효율적인 캐시 정책을 사용하여 정적인 애셋 제공하기
  <details>
  <summary>접기/펼치기</summary>

  ```
  # nginx.conf
  http {
    proxy_cache_path /data/nginx/cache levels=1:2 keys_zone=static:10m inactive=24h max_size=128m;
    
    server {
      location / {
        proxy_pass http://app;
      }

      # assets, media
      location ~* \.(?:css(\.map)?|js(\.map)?|jpe?g|png|gif|ico|cur|heic|webp|tiff?|mp3|m4a|aac|ogg|midi?|wav|mp4|mov|webm|mpe?g|avi|ogv|flv|wmv)$ {
        proxy_pass http://app;
        proxy_cache cache_zone;
        proxy_cache_valid 200 302 20m;
        proxy_cache_valid 404 20m;
        add_header X-Proxy-Cache $upstream_cache_status;
        add_header Cache-Control "public";
        proxy_ignore_headers X-Accel-Expires Expires Cache-Control;
        expires 1y;
        access_log off;
      }

      # svg, fonts
      location ~* \.(?:svgz?|ttf|ttc|otf|eot|woff2?)$ {
        proxy_pass http://app;
        proxy_cache cache_zone;
        proxy_cache_valid 200 302 20m;
        proxy_cache_valid 404 20m;
        add_header X-Proxy-Cache $upstream_cache_status;
        add_header Cache-Control "public";
        proxy_ignore_headers X-Accel-Expires Expires Cache-Control;
        expires 1y;
        access_log off;
      }
    }
  }
  
  # Dockerfile
  FROM nginx
  
  RUN mkdir /etc/nginx/cache
  COPY nginx.conf /etc/nginx/nginx.conf
  COPY fullchain.pem /etc/letsencrypt/live/give928.kro.kr/fullchain.pem
  COPY privkey.pem /etc/letsencrypt/live/give928.kro.kr/privkey.pem
  ```
  ```shell
  $ docker stop proxy && docker rm proxy
  $ docker build -t reverse-proxy:0.0.4 .
  $ docker run -d -p 80:80 -p 443:443 --name proxy -v /var/log/nginx:/var/log/nginx reverse-proxy:0.0.4
  ```
  </details>
  
> 개선 후
>
> 구분 | before | after |  naver
> -----|--------:|------:|-------:
> 성능 |      31 |    57 | 55
> First Contentful Paint |   14.6s |  2.5s | 2.2s
> Time to Interactive |   15.2s |  5.4s | 6.6s
> Speed Index |   14.6s |  4.0s | 7.3s
> Total Blocking Time |   560ms | 500ms | 330ms
> Largest Contentful Paint |   15.2s |  5.5s | 7.4s
> Cumulative Layout Shift |   0.041 | 0.042 | 0.03

#### 3. 부하테스트 전제조건은 어느정도로 설정하셨나요
- 전제 조건 정리
  - 테스트 대상 시스템: 지하철노선도 서비스
  - 예상 1달 사용자 수(MAU) = 3,000,000
  - 예상 1일 사용자 수(DAU) = 100,000
  - 1명당 1일 평균 접속/요청수 = 5
  - 최대 트래픽 = DAU * 1명당 1일 평균 접속/요청수 = 100,000 * 5 = 500,000
  - 평소 트래픽: DAU / 임의의 상수 * 1명당 1일 평균 접속/요청수 = 100,000 / 10 * 5 = 50,000
  - 피크 시간대 집중률: 최대 트래픽 / 평소 트래픽 = 500,000 / 50,000 = 10

- 목표값 설정
  - Throughput
    - 1일 총 접속 수 = DAU / 1명당 1일 평균 접속/요청수 = 100,000 * 5 = 500,000
    - 1일 평균 rps = 1일 총 접속 수 / 1일 초 환산 = 500,000 / 86,400 = 5.79
    - 1일 최대 rps = 1일 평균 rps * 피크 시간대 집중률 = 5.79 * 10 = 57.9
  - Latency: 50 ~ 100ms

#### 4. Smoke, Load, Stress 테스트 스크립트와 결과를 공유해주세요
- 테스트 시나리오
  - 메인 > 가입 > 로그인 > 즐겨찾기 > 경로검색

- 테스트 데이터
  - 지하철노선 11개(1~9호선, 분당선, 신분당선)
  - 지하철역 475개
  - 지하철구간 552개

- 테스트 스크립트
  <details>
  <summary>접기/펼치기</summary>
  
  ```javascript
  import http from 'k6/http';
  import { check } from 'k6';
  
  const url = 'https://give928.kro.kr/';
  
  export const options = {
      // Smoke Test
      vus: 1,
      duration: '3s',
  
      // Load Test
      /*stages: [
          { duration: '10s', target: 50 },
          { duration: '10s', target: 100 },
          { duration: '10s', target: 100 },
          { duration: '10s', target: 100 },
          { duration: '10s', target: 50 },
      ],*/
  
      // Stress Test
      /*stages: [
          { duration: '10s', target: 100 },
          { duration: '10s', target: 200 },
          { duration: '10s', target: 300 },
          { duration: '10s', target: 400 },
          { duration: '10s', target: 100 },
      ],*/
  };
  
  export default function () {
      main();
  
      const email = generateEmail();
      const password = "1234";
      const age = 30;
  
      join(email, password, age);
  
      const accessToken = login(email, password);
  
      favorite(accessToken);
  
      path();
  };
  
  const main = () => {
      const res = http.get(url);
      check(res, { 'main status was 200': (r) => r.status === 200 });
  };
  
  const join = (email, password, age) => {
      const payload = JSON.stringify({
          email: email,
          password: password,
          age: age
      });
  
      const params = {
          headers: {
              'Content-Type': 'application/json',
          },
      };
  
      const res = http.post(url + '/members', payload, params);
      check(res, { 'join status was 201': (r) => r.status === 201 });
  };
  
  const login = (email, password) => {
      const payload = JSON.stringify({
          email: email,
          password: password
      });
  
      const params = {
          headers: {
              'Content-Type': 'application/json',
          },
      };
  
      const res = http.post(url + '/login/token', payload, params);
      check(res, { 'login status was 200': (r) => r.status === 200 });
  
      return res.json('accessToken');
  };
  
  const favorite = (accessToken) => {
      const params = {
          headers: {
              'Authorization': 'Bearer ' + accessToken,
          },
      };
  
      const res = http.get(url + '/favorites', params);
      check(res, { 'favorite status was 200': (r) => r.status === 200 });
  };
  
  const path = () => {
      const res = http.get(url + '/paths?source=113&target=173');
      check(res, { 'path status was 200': (r) => r.status === 200 });
  };
  
  const generateEmail = () => {
      return uuid() + "@test.com"
  };
  
  const uuid = () => {
      return 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
          const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
      });
  };
  ```
  </details>

- Smoke Test
  <details>
  <summary>접기/펼치기</summary>

  - gatling
  ```
  ================================================================================
  ---- Global Information --------------------------------------------------------
  > request count                                          5 (OK=5      KO=0     )
  > min response time                                     28 (OK=28     KO=-     )
  > max response time                                    115 (OK=115    KO=-     )
  > mean response time                                    51 (OK=51     KO=-     )
  > std deviation                                         33 (OK=33     KO=-     )
  > response time 50th percentile                         30 (OK=30     KO=-     )
  > response time 75th percentile                         53 (OK=53     KO=-     )
  > response time 95th percentile                        103 (OK=103    KO=-     )
  > response time 99th percentile                        113 (OK=113    KO=-     )
  > mean requests/sec                                      5 (OK=5      KO=-     )
  ---- Response Time Distribution ------------------------------------------------
  > t < 800 ms                                             5 (100%)
  > 800 ms < t < 1200 ms                                   0 (  0%)
  > t > 1200 ms                                            0 (  0%)
  > failed                                                 0 (  0%)
  ================================================================================
  ```
  
  - k6
  ```
  scenarios: (100.00%) 1 scenario, 1 max VUs, 31s max duration (incl. graceful stop):
  * default: 1 looping VUs for 1s (gracefulStop: 30s)


  running (01.1s), 0/1 VUs, 5 complete and 0 interrupted iterations
  default ✓ [======================================] 1 VUs  1s


    ✓ main status was 200
    ✓ join status was 201
    ✓ login status was 200
    ✓ favorite status was 200
    ✓ path status was 200
    
    checks.........................: 100.00% ✓ 25        ✗ 0  
    data_received..................: 20 kB   18 kB/s
    data_sent......................: 5.7 kB  5.1 kB/s
    http_req_blocked...............: avg=2.23ms   min=1µs      med=2µs      max=55.8ms   p(90)=3µs      p(95)=3µs     
    http_req_connecting............: avg=804.44µs min=0s       med=0s       max=20.11ms  p(90)=0s       p(95)=0s      
    http_req_duration..............: avg=42.6ms   min=10.32ms  med=25.76ms  max=142.65ms p(90)=125.37ms p(95)=134.57ms
      { expected_response:true }...: avg=42.6ms   min=10.32ms  med=25.76ms  max=142.65ms p(90)=125.37ms p(95)=134.57ms
    http_req_failed................: 0.00%   ✓ 0         ✗ 25 
    http_req_receiving.............: avg=39.96µs  min=21µs     med=41µs     max=61µs     p(90)=54.8µs   p(95)=59.99µs 
    http_req_sending...............: avg=20µs     min=5µs      med=11µs     max=235µs    p(90)=15.6µs   p(95)=17.59µs 
    http_req_tls_handshaking.......: avg=1.37ms   min=0s       med=0s       max=34.38ms  p(90)=0s       p(95)=0s      
    http_req_waiting...............: avg=42.54ms  min=10.29ms  med=25.7ms   max=142.58ms p(90)=125.32ms p(95)=134.51ms
    http_reqs......................: 25      22.215271/s
    iteration_duration.............: avg=224.84ms min=192.17ms med=217.69ms max=279.1ms  p(90)=254.86ms p(95)=266.98ms
    iterations.....................: 5       4.443054/s
    vus............................: 1       min=1       max=1
    vus_max........................: 1       min=1       max=1
  ```
  </details>

- Load Test
  <details>
  <summary>접기/펼치기</summary>

  - gatling
  ```
  ================================================================================
  ---- Global Information --------------------------------------------------------
  > request count                                       2000 (OK=2000   KO=0     )
  > min response time                                     12 (OK=12     KO=-     )
  > max response time                                    408 (OK=408    KO=-     )
  > mean response time                                    38 (OK=38     KO=-     )
  > std deviation                                         39 (OK=39     KO=-     )
  > response time 50th percentile                         23 (OK=23     KO=-     )
  > response time 75th percentile                         41 (OK=41     KO=-     )
  > response time 95th percentile                        104 (OK=104    KO=-     )
  > response time 99th percentile                        233 (OK=233    KO=-     )
  > mean requests/sec                                     40 (OK=40     KO=-     )
  ---- Response Time Distribution ------------------------------------------------
  > t < 800 ms                                          2000 (100%)
  > 800 ms < t < 1200 ms                                   0 (  0%)
  > t > 1200 ms                                            0 (  0%)
  > failed                                                 0 (  0%)
  ================================================================================
  ```

  - k6
  ```
  scenarios: (100.00%) 1 scenario, 100 max VUs, 1m20s max duration (incl. graceful stop):
  * default: Up to 100 looping VUs for 50s over 5 stages (gracefulRampDown: 30s, gracefulStop: 30s)


  running (0m52.2s), 000/100 VUs, 706 complete and 0 interrupted iterations
  default ✓ [======================================] 000/100 VUs  50s

    ✓ main status was 200
    ✓ join status was 201
    ✓ login status was 200
    ✓ favorite status was 200
    ✓ path status was 200
  
    checks.........................: 100.00% ✓ 3530      ✗ 0    
    data_received..................: 2.6 MB  50 kB/s
    data_sent......................: 788 kB  15 kB/s
    http_req_blocked...............: avg=811.79µs min=0s     med=1µs   max=63.24ms p(90)=4µs   p(95)=5µs  
    http_req_connecting............: avg=208.61µs min=0s     med=0s    max=15.62ms p(90)=0s    p(95)=0s   
    http_req_duration..............: avg=1.11s    min=7.63ms med=1.1s  max=4.6s    p(90)=2.22s p(95)=2.54s
      { expected_response:true }...: avg=1.11s    min=7.63ms med=1.1s  max=4.6s    p(90)=2.22s p(95)=2.54s
    http_req_failed................: 0.00%   ✓ 0         ✗ 3530 
    http_req_receiving.............: avg=31.06µs  min=7µs    med=25µs  max=277µs   p(90)=57µs  p(95)=77µs 
    http_req_sending...............: avg=11.27µs  min=2µs    med=9µs   max=339µs   p(90)=20µs  p(95)=27µs 
    http_req_tls_handshaking.......: avg=595.75µs min=0s     med=0s    max=42.25ms p(90)=0s    p(95)=0s   
    http_req_waiting...............: avg=1.11s    min=7.6ms  med=1.1s  max=4.6s    p(90)=2.22s p(95)=2.54s
    http_reqs......................: 3530    67.618981/s
    iteration_duration.............: avg=5.55s    min=2.14s  med=5.79s max=9.2s    p(90)=6.76s p(95)=6.95s
    iterations.....................: 706     13.523796/s
    vus............................: 14      min=5       max=100
    vus_max........................: 100     min=100     max=100
  ```
  </details>

- Stress Test
  - 프리티어 t2.micro(1 vCPU, 1 GiB) 에서 한계치가 낮아서 기능이 정상 동작하지 않는다.

  <details>
  <summary>접기/펼치기</summary>

  - gatling
  ```
  ================================================================================
  ---- Global Information --------------------------------------------------------
  > request count                                       4611 (OK=1111   KO=3500  )
  > min response time                                     11 (OK=11     KO=10000 )
  > max response time                                  60002 (OK=56869  KO=60002 )
  > mean response time                                  8675 (OK=711    KO=11203 )
  > std deviation                                       8155 (OK=2832   KO=7652  )
  > response time 50th percentile                      10001 (OK=38     KO=10001 )
  > response time 75th percentile                      10002 (OK=111    KO=10002 )
  > response time 95th percentile                      10011 (OK=6316   KO=10012 )
  > response time 99th percentile                      60001 (OK=6672   KO=60001 )
  > mean requests/sec                                 30.946 (OK=7.456  KO=23.49 )
  ---- Response Time Distribution ------------------------------------------------
  > t < 800 ms                                          1000 ( 22%)
  > 800 ms < t < 1200 ms                                   7 (  0%)
  > t > 1200 ms                                          104 (  2%)
  > failed                                              3500 ( 76%)
  ---- Errors --------------------------------------------------------------------
  > i.n.c.ConnectTimeoutException: connection timed out: give928.k   2900 (66.07%)
  ro.kr/13.125.12.136:443
  > favorite: Failed to build request: Failed to build request: j.    889 (20.26%)
  u.NoSuchElementException: No attribute named 'accessToken' is ...
  > j.n.s.SSLException: handshake timed out                           516 (11.76%)
  > i.g.h.c.i.RequestTimeoutException: Request timeout to give928.     84 ( 1.91%)
  kro.kr/13.125.12.136:443 after 60000 ms
  ================================================================================
  ```

  - k6
  ```
  scenarios: (100.00%) 1 scenario, 400 max VUs, 1m20s max duration (incl. graceful stop):
  * default: Up to 400 looping VUs for 50s over 5 stages (gracefulRampDown: 30s, gracefulStop: 30s)
  
  
  running (1m20.0s), 000/400 VUs, 178 complete and 320 interrupted iterations
  default ✓ [======================================] 038/400 VUs  50s

    ✓ main status was 200
    ✗ join status was 201
    ↳  99% — ✓ 386 / ✗ 1
    ✗ login status was 200
    ↳  97% — ✓ 260 / ✗ 6
    ✗ favorite status was 200
    ↳  95% — ✓ 238 / ✗ 10
    ✗ path status was 200
    ↳  91% — ✓ 163 / ✗ 15

    checks.........................: 97.97% ✓ 1545      ✗ 32
    data_received..................: 2.9 MB 37 kB/s
    data_sent......................: 551 kB 6.9 kB/s
    http_req_blocked...............: avg=4.5ms    min=1.57µs  med=5.96µs   max=145.35ms p(90)=17.26ms  p(95)=17.92ms
    http_req_connecting............: avg=1.15ms   min=0s      med=0s       max=21.7ms   p(90)=4.5ms    p(95)=4.84ms
    http_req_duration..............: avg=10.25s   min=4.49ms  med=5.31s    max=45.25s   p(90)=29.7s    p(95)=33.96s
      { expected_response:true }...: avg=9.82s    min=4.49ms  med=5.3s     max=45.25s   p(90)=27.33s   p(95)=33.96s
    http_req_failed................: 2.02%  ✓ 32        ✗ 1545
    http_req_receiving.............: avg=104.15µs min=27.12µs med=102.01µs max=219.67µs p(90)=146.85µs p(95)=158.83µs
    http_req_sending...............: avg=57.08µs  min=8.78µs  med=43.75µs  max=178.03µs p(90)=115.35µs p(95)=125.63µs
    http_req_tls_handshaking.......: avg=3.24ms   min=0s      med=0s       max=54.45ms  p(90)=12.62ms  p(95)=13.24ms
    http_req_waiting...............: avg=10.25s   min=4.35ms  med=5.31s    max=45.25s   p(90)=29.7s    p(95)=33.96s
    http_reqs......................: 1577   19.711627/s
    iteration_duration.............: avg=35.38s   min=3.93s   med=38.9s    max=1m9s     p(90)=55.94s   p(95)=59.48s
    iterations.....................: 178    2.224901/s
    vus............................: 69     min=10      max=400
    vus_max........................: 400    min=400     max=400
  ```
  </details>
