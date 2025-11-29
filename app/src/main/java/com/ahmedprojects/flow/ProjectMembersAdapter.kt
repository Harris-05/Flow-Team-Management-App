package com.ahmedprojects.flow

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ProjectMembersAdapter(
    private val members: List<ProjectMembers>
) : RecyclerView.Adapter<ProjectMembersAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_name)
        val email: TextView = view.findViewById(R.id.tv_email)
        val role: TextView = view.findViewById(R.id.tv_role)
        val emailButton: ImageView = view.findViewById(R.id.btn_email)
        val card: CardView = view.findViewById(R.id.main)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.people_card, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]

        holder.name.text = member.name
        holder.email.text = member.email
        holder.role.text = member.role.capitalize()

        // Owner badge color is already handled by your drawable
        if (member.role == "owner") {
            holder.role.setBackgroundResource(R.drawable.bg_owner_badge)
            holder.role.setTextColor(0xFF7B1EFF.toInt())
        } else {
            holder.role.setBackgroundResource(R.drawable.bg_member_badge)
            holder.role.setTextColor(0xFF2563EB.toInt())
        }

        // Click → Go to other profile page
        /*holder.card.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, other_profile_page::class.java)
            intent.putExtra("user_id", member.userId)
            context.startActivity(intent)
        }*/

        // Email icon (optional click)
        holder.emailButton.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.type = "message/rfc822"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(member.email))
            holder.itemView.context.startActivity(
                Intent.createChooser(emailIntent, "Send Email")
            )
        }
    }

    override fun getItemCount(): Int = members.size
}
