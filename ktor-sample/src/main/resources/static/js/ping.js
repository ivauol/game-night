function ping_interval(token){
    let interval = null;

    async function ping(token){
        const response = await fetch(`/ping?token=${token}`);
        const text = await response.text();
        if (text === "Session Expired."){
            const banner = document.createElement("div");
            banner.innerHTML = `Session Expired. <a href="/gamecenter">Return to home</a>`;
            document.body.appendChild(banner);
            clearInterval(interval);
        }
    }

    interval = setInterval(() => ping(token), 15000);
}